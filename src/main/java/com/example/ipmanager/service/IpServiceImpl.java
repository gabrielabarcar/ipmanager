package com.example.ipmanager.service;

import com.example.ipmanager.model.Network;
import com.example.ipmanager.repository.NetworkRepository;
import com.example.ipmanager.util.InputSecurityUtil;
import com.example.ipmanager.util.IpUtils;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class IpServiceImpl implements IpService {

    private final NetworkRepository networkRepository;

    public IpServiceImpl(NetworkRepository networkRepository) {
        this.networkRepository = networkRepository;
    }

    @Override
    public List<Network> findAllOrdered() {
        List<Network> networks = networkRepository.findAllByOrderByIdDesc();
        marcarConflictos(networks);
        asignarDisplayIndexDesc(networks);
        return networks;
    }

    @Override
    public Network save(Network network) {
        normalizar(network);
        sanitizar(network);
        validarCamposObligatorios(network);
        validarEnlaceInterno(network);
        autocompletarWan(network);

        if (network.getEsHistorico() == null) {
            network.setEsHistorico(false);
        }

        if (network.getAceptaTraslape() == null) {
            network.setAceptaTraslape(false);
        }

        ResultadoTraslape resultado = validarTraslapesGuardado(network);

        if (resultado.hayConflicto) {
            if (!Boolean.TRUE.equals(network.getAceptaTraslape())) {
                throw new RuntimeException(resultado.detalle);
            }

            if (network.getMotivoJustificacion() == null || network.getMotivoJustificacion().isBlank()) {
                throw new RuntimeException("Debe indicar una justificación para guardar un registro con traslape.");
            }

            network.setTraslapeAceptadoPor(getCurrentUsername());
            network.setTraslapeAceptadoAt(LocalDateTime.now());
        } else {
            network.setAceptaTraslape(false);
            network.setMotivoJustificacion(null);
            network.setTraslapeAceptadoPor(null);
            network.setTraslapeAceptadoAt(null);
        }

        return networkRepository.save(network);
    }

    @Override
    public void deleteById(Long id) {
        networkRepository.deleteById(id);
    }

    @Override
    public List<Network> importCsvEstricto(MultipartFile file) throws IOException {
        List<Network> existentes = networkRepository.findAll();
        List<Network> importados = parseCsv(file, false);
        List<Network> base = new ArrayList<>(existentes);

        for (Network n : importados) {
            normalizar(n);
            sanitizar(n);
            validarCamposObligatorios(n);
            validarEnlaceInterno(n);
            autocompletarWan(n);

            ResultadoTraslape resultado = validarContraLista(n, base);
            if (resultado.hayConflicto) {
                throw new RuntimeException(resultado.detalle);
            }

            base.add(n);
        }

        for (Network n : importados) {
            networkRepository.save(n);
        }

        return importados;
    }

    @Override
    public List<Network> importCsvHistorico(MultipartFile file) throws IOException {
        List<Network> importados = parseCsv(file, true);

        for (Network n : importados) {
            normalizar(n);
            sanitizar(n);
            if (n.getWanIp() != null && !n.getWanIp().isBlank()) {
                autocompletarWan(n);
            }
            networkRepository.save(n);
        }

        return importados;
    }

    @Override
    public Map<String, Object> validateEnlace(String enlace, Long id) {
        Map<String, Object> res = new HashMap<>();
        res.put("ok", true);
        res.put("message", "");

        if (enlace == null || enlace.isBlank()) {
            return res;
        }

        enlace = InputSecurityUtil.sanitizeEnlace(enlace);

        if (!enlace.matches("\\d{1,8}")) {
            res.put("ok", false);
            res.put("message", "El enlace solo permite números.");
            return res;
        }

        if (enlace.length() < 8) {
            res.put("ok", false);
            res.put("message", "El enlace debe tener 8 dígitos.");
            return res;
        }

        boolean existe = (id == null)
                ? networkRepository.existsByEnlace(enlace)
                : networkRepository.existsByEnlaceAndIdNot(enlace, id);

        if (existe) {
            res.put("ok", false);
            res.put("message", "Ese número de enlace ya existe, debes cambiarlo.");
            return res;
        }

        res.put("message", "Enlace disponible.");
        return res;
    }

    @Override
    public Map<String, Object> validateLan(String ip, Integer cidr, Long id) {
        return validateNetworkRealtime(ip, cidr, id, true);
    }

    @Override
    public Map<String, Object> validateWan(String ip, Integer cidr, Long id) {
        return validateNetworkRealtime(ip, cidr, id, false);
    }

    @Override
    public String obtenerSiguienteIpDisponible(String tipo) {
        boolean esLan = !"wan".equalsIgnoreCase(tipo);

        List<Network> existentes = networkRepository.findAll();
        List<RangoIp> rangos = new ArrayList<>();

        for (Network n : existentes) {
            String ip = esLan ? n.getNetworkIp() : n.getWanIp();
            Integer cidr = esLan ? n.getCidr() : n.getWanCidr();

            if (ip == null || ip.isBlank() || cidr == null) {
                continue;
            }

            if (IpUtils.isZeroIp(ip)) {
                continue;
            }

            long inicio = IpUtils.networkAddress(ip, cidr);
            long fin = IpUtils.broadcastAddress(ip, cidr);

            rangos.add(new RangoIp(inicio, fin));
        }

        if (rangos.isEmpty()) {
            return esLan ? "10.0.0.0" : "192.168.0.0";
        }

        rangos.sort(Comparator.comparingLong(r -> r.inicio));

        List<RangoIp> fusionados = new ArrayList<>();
        RangoIp actual = rangos.get(0);

        for (int i = 1; i < rangos.size(); i++) {
            RangoIp siguiente = rangos.get(i);

            if (siguiente.inicio <= actual.fin + 1) {
                actual.fin = Math.max(actual.fin, siguiente.fin);
            } else {
                fusionados.add(actual);
                actual = siguiente;
            }
        }
        fusionados.add(actual);

        long candidato = fusionados.get(0).fin + 1;

        for (int i = 1; i < fusionados.size(); i++) {
            RangoIp previo = fusionados.get(i - 1);
            RangoIp siguiente = fusionados.get(i);

            long hueco = previo.fin + 1;
            if (hueco < siguiente.inicio) {
                return IpUtils.longToIp(hueco);
            }

            if (siguiente.fin + 1 > candidato) {
                candidato = siguiente.fin + 1;
            }
        }

        if (candidato < 0 || candidato > 4294967295L) {
            return "0.0.0.0";
        }

        return IpUtils.longToIp(candidato);
    }

    private Map<String, Object> validateNetworkRealtime(String ip, Integer cidr, Long id, boolean lan) {
        Map<String, Object> res = new HashMap<>();
        res.put("ok", true);
        res.put("message", "");
        res.put("explanation", "");

        if (ip == null || ip.isBlank() || cidr == null) {
            return res;
        }

        ip = InputSecurityUtil.sanitizeIp(ip, lan ? "LAN IP" : "WAN IP");
        InputSecurityUtil.validateCidr(cidr, lan ? "Máscara LAN" : "Máscara WAN");

        if (IpUtils.isZeroIp(ip)) {
            return res;
        }

        try {
            IpUtils.ipToLong(ip);
        } catch (Exception e) {
            res.put("ok", false);
            res.put("message", "IP inválida.");
            return res;
        }

        List<Network> existentes = networkRepository.findAll();
        List<String> summaries = new ArrayList<>();
        List<String> explanations = new ArrayList<>();

        for (Network n : existentes) {
            if (id != null && n.getId() != null && id.equals(n.getId())) {
                continue;
            }

            if (lan) {
                if (n.getNetworkIp() != null && n.getCidr() != null && !IpUtils.isZeroIp(n.getNetworkIp())) {
                    if (IpUtils.overlaps(ip, cidr, n.getNetworkIp(), n.getCidr())) {
                        summaries.add("Traslapa con LAN existente: " + n.getNetworkIp() + "/" + n.getCidr());
                        explanations.add(buildTechnicalExplanation(ip, cidr, n.getNetworkIp(), n.getCidr()));
                    }
                }
            } else {
                if (n.getWanIp() != null && n.getWanCidr() != null && !IpUtils.isZeroIp(n.getWanIp())) {
                    if (IpUtils.overlaps(ip, cidr, n.getWanIp(), n.getWanCidr())) {
                        summaries.add("Traslapa con WAN existente: " + n.getWanIp() + "/" + n.getWanCidr());
                        explanations.add(buildTechnicalExplanation(ip, cidr, n.getWanIp(), n.getWanCidr()));
                    }
                }
            }
        }

        if (!summaries.isEmpty()) {
            res.put("ok", false);
            res.put("message", summaries.get(0));
            res.put("explanation", String.join("\n\n────────────────────────\n\n", explanations));
            return res;
        }

        res.put("message", "Sin conflicto.");
        return res;
    }

    private void normalizar(Network network) {
        if (network.getNombreLugar() != null) {
            network.setNombreLugar(network.getNombreLugar().trim());
        }
        if (network.getEnlace() != null) {
            network.setEnlace(network.getEnlace().trim());
            if (network.getEnlace().isBlank()) {
                network.setEnlace(null);
            }
        }
        if (network.getNetworkIp() != null) {
            network.setNetworkIp(network.getNetworkIp().trim());
        }
        if (network.getWanIp() != null) {
            network.setWanIp(network.getWanIp().trim());
        }
        if (network.getMotivoJustificacion() != null) {
            network.setMotivoJustificacion(network.getMotivoJustificacion().trim());
            if (network.getMotivoJustificacion().isBlank()) {
                network.setMotivoJustificacion(null);
            }
        }
    }

    private void sanitizar(Network network) {
        network.setNombreLugar(InputSecurityUtil.sanitizeGeneralText(network.getNombreLugar(), "Nombre del lugar"));

        if (network.getEnlace() != null) {
            network.setEnlace(InputSecurityUtil.sanitizeEnlace(network.getEnlace()));
        }

        if (network.getNetworkIp() != null) {
            network.setNetworkIp(InputSecurityUtil.sanitizeIp(network.getNetworkIp(), "LAN IP"));
        }

        if (network.getWanIp() != null) {
            network.setWanIp(InputSecurityUtil.sanitizeIp(network.getWanIp(), "WAN IP"));
        }

        if (network.getMotivoJustificacion() != null) {
            network.setMotivoJustificacion(
                    InputSecurityUtil.sanitizeGeneralText(network.getMotivoJustificacion(), "Justificación")
            );
        }
    }

    private void validarCamposObligatorios(Network network) {
        if (network.getNombreLugar() == null || network.getNombreLugar().isBlank()) {
            throw new RuntimeException("El nombre del lugar es obligatorio.");
        }
        if (network.getNetworkIp() == null || network.getNetworkIp().isBlank()) {
            throw new RuntimeException("La LAN IP es obligatoria.");
        }
        InputSecurityUtil.validateCidr(network.getCidr(), "Máscara LAN");

        if (network.getWanIp() == null || network.getWanIp().isBlank()) {
            throw new RuntimeException("La WAN IP es obligatoria.");
        }
        InputSecurityUtil.validateCidr(network.getWanCidr(), "Máscara WAN");

        if (!IpUtils.isZeroIp(network.getNetworkIp())) {
            IpUtils.ipToLong(network.getNetworkIp());
        }
        if (!IpUtils.isZeroIp(network.getWanIp())) {
            IpUtils.ipToLong(network.getWanIp());
        }
    }

    private void validarEnlaceInterno(Network network) {
        if (network.getEnlace() == null || network.getEnlace().isBlank()) {
            return;
        }

        if (!network.getEnlace().matches("\\d{8}")) {
            throw new RuntimeException("El enlace debe tener exactamente 8 dígitos.");
        }

        boolean existe = (network.getId() == null)
                ? networkRepository.existsByEnlace(network.getEnlace())
                : networkRepository.existsByEnlaceAndIdNot(network.getEnlace(), network.getId());

        if (existe) {
            throw new RuntimeException("El número de enlace ya existe.");
        }
    }

    private void autocompletarWan(Network network) {
        if (network.getWanIp() == null || network.getWanIp().isBlank()) {
            return;
        }

        if (IpUtils.isZeroIp(network.getWanIp())) {
            network.setWanIp1("0.0.0.1");
            network.setWanIp2("0.0.0.2");
            network.setWanBroadcast("0.0.0.3");
            return;
        }

        IpUtils.ipToLong(network.getWanIp());

        network.setWanIp1(IpUtils.plusOctet(network.getWanIp(), 1));
        network.setWanIp2(IpUtils.plusOctet(network.getWanIp1(), 1));
        network.setWanBroadcast(IpUtils.plusOctet(network.getWanIp2(), 1));
    }

    private ResultadoTraslape validarTraslapesGuardado(Network actual) {
        List<Network> existentes = networkRepository.findAll();
        return validarContraLista(actual, existentes);
    }

    private ResultadoTraslape validarContraLista(Network actual, List<Network> existentes) {
        if (IpUtils.isZeroIp(actual.getNetworkIp()) && IpUtils.isZeroIp(actual.getWanIp())) {
            return ResultadoTraslape.sinConflicto();
        }

        List<String> conflictos = new ArrayList<>();

        for (Network n : existentes) {
            if (actual.getId() != null && n.getId() != null && actual.getId().equals(n.getId())) {
                continue;
            }

            if (!IpUtils.isZeroIp(actual.getNetworkIp())) {
                if (n.getNetworkIp() != null && n.getCidr() != null && !IpUtils.isZeroIp(n.getNetworkIp())) {
                    if (IpUtils.overlaps(actual.getNetworkIp(), actual.getCidr(), n.getNetworkIp(), n.getCidr())) {
                        conflictos.add(
                                "La LAN IP " + actual.getNetworkIp() + "/" + actual.getCidr()
                                        + " traslapa con la LAN existente "
                                        + n.getNetworkIp() + "/" + n.getCidr()
                        );
                    }
                }
            }

            if (!IpUtils.isZeroIp(actual.getWanIp())) {
                if (n.getWanIp() != null && n.getWanCidr() != null && !IpUtils.isZeroIp(n.getWanIp())) {
                    if (IpUtils.overlaps(actual.getWanIp(), actual.getWanCidr(), n.getWanIp(), n.getWanCidr())) {
                        conflictos.add(
                                "La WAN IP " + actual.getWanIp() + "/" + actual.getWanCidr()
                                        + " traslapa con la WAN existente "
                                        + n.getWanIp() + "/" + n.getWanCidr()
                        );
                    }
                }
            }
        }

        if (conflictos.isEmpty()) {
            return ResultadoTraslape.sinConflicto();
        }

        return ResultadoTraslape.conConflicto(String.join(" | ", conflictos));
    }

    private List<Network> parseCsv(MultipartFile file, boolean historico) throws IOException {
        List<Network> lista = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {

            String line;
            boolean primeraLinea = true;

            while ((line = br.readLine()) != null) {
                if (line.isBlank()) {
                    continue;
                }

                if (primeraLinea) {
                    primeraLinea = false;
                    String encabezado = line.toLowerCase();
                    if (encabezado.contains("nombre") || encabezado.contains("networkip") || encabezado.contains("lugar")) {
                        continue;
                    }
                }

                String[] p = line.split(",", -1);

                Network n = new Network();
                n.setEsHistorico(historico);
                n.setNombreLugar(getValue(p, 0));
                n.setNetworkIp(getValue(p, 1));
                n.setCidr(parseInteger(getValue(p, 2)));
                n.setWanIp(getValue(p, 3));
                n.setWanCidr(parseInteger(getValue(p, 4)));
                n.setWanIp1(getValue(p, 5));
                n.setWanIp2(getValue(p, 6));
                n.setWanBroadcast(getValue(p, 7));
                n.setEnlace(getValue(p, 8));

                lista.add(n);
            }
        }

        return lista;
    }

    private void marcarConflictos(List<Network> networks) {
        for (Network n : networks) {
            n.setTieneConflicto(false);
            n.setDetalleConflicto(null);
            n.setTieneLanConflicto(false);
            n.setTieneWanConflicto(false);
            n.setLanConflictExplanation(null);
            n.setWanConflictExplanation(null);
        }

        for (int i = 0; i < networks.size(); i++) {
            Network a = networks.get(i);

            for (int j = i + 1; j < networks.size(); j++) {
                Network b = networks.get(j);

                if (a.getNetworkIp() != null && b.getNetworkIp() != null &&
                        a.getCidr() != null && b.getCidr() != null &&
                        !IpUtils.isZeroIp(a.getNetworkIp()) && !IpUtils.isZeroIp(b.getNetworkIp())) {

                    if (IpUtils.overlaps(a.getNetworkIp(), a.getCidr(), b.getNetworkIp(), b.getCidr())) {
                        String shortA = "Traslapa LAN con " + b.getNetworkIp() + "/" + b.getCidr();
                        String shortB = "Traslapa LAN con " + a.getNetworkIp() + "/" + a.getCidr();

                        String expA = buildTechnicalExplanation(a.getNetworkIp(), a.getCidr(), b.getNetworkIp(), b.getCidr());
                        String expB = buildTechnicalExplanation(b.getNetworkIp(), b.getCidr(), a.getNetworkIp(), a.getCidr());

                        registrarConflictoLan(a, shortA, expA);
                        registrarConflictoLan(b, shortB, expB);
                    }
                }

                if (a.getWanIp() != null && b.getWanIp() != null &&
                        a.getWanCidr() != null && b.getWanCidr() != null &&
                        !IpUtils.isZeroIp(a.getWanIp()) && !IpUtils.isZeroIp(b.getWanIp())) {

                    if (IpUtils.overlaps(a.getWanIp(), a.getWanCidr(), b.getWanIp(), b.getWanCidr())) {
                        String shortA = "Traslapa WAN con " + b.getWanIp() + "/" + b.getWanCidr();
                        String shortB = "Traslapa WAN con " + a.getWanIp() + "/" + a.getWanCidr();

                        String expA = buildTechnicalExplanation(a.getWanIp(), a.getWanCidr(), b.getWanIp(), b.getWanCidr());
                        String expB = buildTechnicalExplanation(b.getWanIp(), b.getWanCidr(), a.getWanIp(), a.getWanCidr());

                        registrarConflictoWan(a, shortA, expA);
                        registrarConflictoWan(b, shortB, expB);
                    }
                }
            }
        }
    }

    private void registrarConflictoLan(Network n, String detalleCorto, String explicacion) {
        n.setTieneConflicto(true);
        n.setTieneLanConflicto(true);
        n.setDetalleConflicto(appendUnique(n.getDetalleConflicto(), detalleCorto, " | "));
        n.setLanConflictExplanation(appendUnique(n.getLanConflictExplanation(), explicacion, "\n\n────────────────────────\n\n"));
    }

    private void registrarConflictoWan(Network n, String detalleCorto, String explicacion) {
        n.setTieneConflicto(true);
        n.setTieneWanConflicto(true);
        n.setDetalleConflicto(appendUnique(n.getDetalleConflicto(), detalleCorto, " | "));
        n.setWanConflictExplanation(appendUnique(n.getWanConflictExplanation(), explicacion, "\n\n────────────────────────\n\n"));
    }

    private String appendUnique(String actual, String nuevo, String separador) {
        if (nuevo == null || nuevo.isBlank()) {
            return actual;
        }
        if (actual == null || actual.isBlank()) {
            return nuevo;
        }
        if (actual.contains(nuevo)) {
            return actual;
        }
        return actual + separador + nuevo;
    }

    private String buildTechnicalExplanation(String ipA, int cidrA, String ipB, int cidrB) {
        long netA = IpUtils.networkAddress(ipA, cidrA);
        long broadA = IpUtils.broadcastAddress(ipA, cidrA);

        long netB = IpUtils.networkAddress(ipB, cidrB);
        long broadB = IpUtils.broadcastAddress(ipB, cidrB);

        String redIngresada = ipA + "/" + cidrA;
        String redComparada = ipB + "/" + cidrB;

        String redBaseA = IpUtils.longToIp(netA) + "/" + cidrA;
        String redBaseB = IpUtils.longToIp(netB) + "/" + cidrB;

        String rangoA = IpUtils.longToIp(netA) + " hasta " + IpUtils.longToIp(broadA);
        String rangoB = IpUtils.longToIp(netB) + " hasta " + IpUtils.longToIp(broadB);

        String motivo;

        if (netA >= netB && broadA <= broadB) {
            motivo = "Porque la red /" + cidrA + " está contenida dentro de la red /" + cidrB + ".";
        } else if (netB >= netA && broadB <= broadA) {
            motivo = "Porque la red /" + cidrB + " está contenida dentro de la red /" + cidrA + ".";
        } else {
            motivo = "Porque ambas redes comparten una parte del rango de direcciones.";
        }

        return "¿" + redIngresada + " traslapa con " + redComparada + "?\n\n"
                + "Sí.\n\n"
                + redComparada + " pertenece a la red:\n\n"
                + redBaseB + "\n\n"
                + "Y " + redIngresada + " cae dentro de ese rango, porque:\n\n"
                + "• " + redBaseB + " cubre desde " + rangoB + "\n"
                + "• " + redBaseA + " cubre desde " + rangoA + "\n\n"
                + "Entonces:\n\n"
                + redIngresada + " sí traslapa con " + redComparada + "\n\n"
                + motivo;
    }

    private void asignarDisplayIndexDesc(List<Network> networks) {
        int total = networks.size();
        for (int i = 0; i < total; i++) {
            networks.get(i).setDisplayIndex(total - i);
        }
    }

    private String getValue(String[] arr, int index) {
        if (index >= arr.length) {
            return null;
        }
        String value = arr[index];
        return value == null ? null : value.trim();
    }

    private Integer parseInteger(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return Integer.parseInt(value.trim());
    }

    private String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
            return "sistema";
        }
        return authentication.getName();
    }

    private static class RangoIp {
        private long inicio;
        private long fin;

        private RangoIp(long inicio, long fin) {
            this.inicio = inicio;
            this.fin = fin;
        }
    }

    private static class ResultadoTraslape {
        private final boolean hayConflicto;
        private final String detalle;

        private ResultadoTraslape(boolean hayConflicto, String detalle) {
            this.hayConflicto = hayConflicto;
            this.detalle = detalle;
        }

        private static ResultadoTraslape sinConflicto() {
            return new ResultadoTraslape(false, "");
        }

        private static ResultadoTraslape conConflicto(String detalle) {
            return new ResultadoTraslape(true, detalle);
        }
    }
}
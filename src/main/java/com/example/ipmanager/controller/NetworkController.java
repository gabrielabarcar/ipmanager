package com.example.ipmanager.controller;

import com.example.ipmanager.model.Network;
import com.example.ipmanager.service.AuditLogService;
import com.example.ipmanager.service.IpService;
import com.lowagie.text.Document;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.lowagie.text.Element;
import com.lowagie.text.HeaderFooter;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Controller
public class NetworkController {

    private final IpService ipService;
    private final AuditLogService auditLogService;

    public NetworkController(IpService ipService, AuditLogService auditLogService) {
        this.ipService = ipService;
        this.auditLogService = auditLogService;
    }

    @GetMapping("/")
    public String index(Model model) {
        List<Network> networks = ipService.findAllOrdered();

        long totalRegistros = networks.size();
        long totalConflictos = networks.stream()
                .filter(n -> Boolean.TRUE.equals(n.getTieneConflicto()))
                .count();

        model.addAttribute("network", new Network());
        model.addAttribute("networks", networks);
        model.addAttribute("totalRegistros", totalRegistros);
        model.addAttribute("totalConflictos", totalConflictos);

        return "index";
    }

    @PostMapping("/save")
public String save(@ModelAttribute("network") Network network,
                   @RequestParam(value = "postSaveAction", required = false) String postSaveAction,
                   RedirectAttributes redirectAttributes) {
                try {
            boolean isEdit = network.getId() != null;
            boolean aceptaTraslape = Boolean.TRUE.equals(network.getAceptaTraslape());

            String detalleBase = (isEdit
                    ? "Actualización de red en lugar: "
                    : "Creación de red en lugar: ")
                    + value(network.getNombreLugar())
                    + " | LAN=" + value(network.getNetworkIp()) + "/" + value(network.getCidr())
                    + " | WAN=" + value(network.getWanIp()) + "/" + value(network.getWanCidr());

            network.setEsHistorico(false);
            Network saved = ipService.save(network);

            String accion = isEdit ? "UPDATE" : "CREATE";
            String mensaje = isEdit
                    ? "Registro actualizado correctamente."
                    : "Registro guardado correctamente.";

            if (aceptaTraslape) {
                mensaje += " Se registró la justificación de traslape.";
            }

            StringBuilder detalleAuditoria = new StringBuilder(detalleBase);

            if (Boolean.TRUE.equals(saved.getAceptaTraslape())) {
                detalleAuditoria.append(" | TRASLAPE_ACEPTADO=Sí");
                detalleAuditoria.append(" | JUSTIFICACION=").append(value(saved.getMotivoJustificacion()));
                detalleAuditoria.append(" | ACEPTADO_POR=").append(value(saved.getTraslapeAceptadoPor()));
                detalleAuditoria.append(" | FECHA_ACEPTACION=").append(value(saved.getTraslapeAceptadoAt()));
            } else {
                detalleAuditoria.append(" | TRASLAPE_ACEPTADO=No");
            }

            auditLogService.log(
                    "NETWORK",
                    saved.getId(),
                    accion,
                    detalleAuditoria.toString()
            );

            redirectAttributes.addFlashAttribute("success", mensaje);

            if ("email".equalsIgnoreCase(postSaveAction)) {
                String lan = "";
                if (saved.getNetworkIp() != null && saved.getCidr() != null) {
                    lan = saved.getNetworkIp() + "/" + saved.getCidr();
                }

                String wan = "";
                if (saved.getWanIp() != null && saved.getWanCidr() != null) {
                    wan = saved.getWanIp() + "/" + saved.getWanCidr();
                }

                redirectAttributes.addFlashAttribute("emailNombreLugar", value(saved.getNombreLugar()));
                redirectAttributes.addFlashAttribute("emailLan", lan);
                redirectAttributes.addFlashAttribute("emailWan", wan);
                redirectAttributes.addFlashAttribute("emailWan1", value(saved.getWanIp1()));
                redirectAttributes.addFlashAttribute("emailWan2", value(saved.getWanIp2()));
                redirectAttributes.addFlashAttribute("emailBroadcast", value(saved.getWanBroadcast()));
                redirectAttributes.addFlashAttribute("emailAutoOpen", true);
            }

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/";
    }

    @PostMapping("/delete")
    public String delete(@RequestParam("id") Long id,
                         RedirectAttributes redirectAttributes) {
        try {
            auditLogService.log(
                    "NETWORK",
                    id,
                    "DELETE",
                    "Eliminación de red con id=" + id
            );

            ipService.deleteById(id);
            redirectAttributes.addFlashAttribute("success", "Registro eliminado correctamente.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "No fue posible eliminar el registro.");
        }
        return "redirect:/";
    }

    @PostMapping("/import-estricto")
    public String importEstricto(@RequestParam("file") MultipartFile file,
                                 RedirectAttributes redirectAttributes) {
        try {
            List<Network> importados = ipService.importCsvEstricto(file);

            auditLogService.log(
                    "NETWORK",
                    null,
                    "IMPORT_ESTRICTO",
                    "Importación estricta de CSV. Registros importados: " + importados.size()
            );

            redirectAttributes.addFlashAttribute("success", "CSV estricto importado correctamente.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error al importar CSV estricto: " + e.getMessage());
        }
        return "redirect:/";
    }

    @PostMapping("/import-historico")
    public String importHistorico(@RequestParam("file") MultipartFile file,
                                  RedirectAttributes redirectAttributes) {
        try {
            List<Network> importados = ipService.importCsvHistorico(file);

            auditLogService.log(
                    "NETWORK",
                    null,
                    "IMPORT_HISTORICO",
                    "Importación histórica de CSV. Registros importados: " + importados.size()
            );

            redirectAttributes.addFlashAttribute("success", "CSV histórico importado correctamente.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error al importar CSV histórico: " + e.getMessage());
        }
        return "redirect:/";
    }

    @GetMapping("/export/csv")
    public void exportCsv(HttpServletResponse response) throws IOException {
        List<Network> networks = ipService.findAllOrdered();

        response.setContentType("text/csv; charset=UTF-8");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setHeader("Content-Disposition", "attachment; filename=redes.csv");

        StringBuilder sb = new StringBuilder();
        sb.append("ID_MOSTRAR,LUGAR,ENLACE,LAN_IP,MASCARA_LAN,WAN_IP,MASCARA_WAN,WAN+1,WAN+2,WAN_BROADCAST,ES_HISTORICO,TIENE_CONFLICTO,DETALLE_CONFLICTO,ACEPTA_TRASLAPE,MOTIVO_JUSTIFICACION,TRASLAPE_ACEPTADO_POR,TRASLAPE_ACEPTADO_AT,UPDATED_BY,UPDATED_AT\n");

        for (Network n : networks) {
            sb.append(csv(n.getDisplayIndex())).append(",")
                    .append(csv(n.getNombreLugar())).append(",")
                    .append(csv(n.getEnlace())).append(",")
                    .append(csv(n.getNetworkIp())).append(",")
                    .append(csv(n.getCidr())).append(",")
                    .append(csv(n.getWanIp())).append(",")
                    .append(csv(n.getWanCidr())).append(",")
                    .append(csv(n.getWanIp1())).append(",")
                    .append(csv(n.getWanIp2())).append(",")
                    .append(csv(n.getWanBroadcast())).append(",")
                    .append(csv(Boolean.TRUE.equals(n.getEsHistorico()) ? "Sí" : "No")).append(",")
                    .append(csv(Boolean.TRUE.equals(n.getTieneConflicto()) ? "Sí" : "No")).append(",")
                    .append(csv(n.getDetalleConflicto())).append(",")
                    .append(csv(Boolean.TRUE.equals(n.getAceptaTraslape()) ? "Sí" : "No")).append(",")
                    .append(csv(n.getMotivoJustificacion())).append(",")
                    .append(csv(n.getTraslapeAceptadoPor())).append(",")
                    .append(csv(n.getTraslapeAceptadoAt())).append(",")
                    .append(csv(n.getUpdatedBy())).append(",")
                    .append(csv(n.getUpdatedAt()))
                    .append("\n");
        }

        response.getWriter().write(sb.toString());
        response.getWriter().flush();
    }

    @GetMapping("/export/excel")
    public void exportExcel(HttpServletResponse response) throws IOException {
        List<Network> networks = ipService.findAllOrdered();

        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=redes.xlsx");

        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Redes");

        Row header = sheet.createRow(0);
        header.createCell(0).setCellValue("ID");
        header.createCell(1).setCellValue("Lugar");
        header.createCell(2).setCellValue("Enlace");
        header.createCell(3).setCellValue("LAN IP");
        header.createCell(4).setCellValue("Máscara LAN");
        header.createCell(5).setCellValue("WAN IP");
        header.createCell(6).setCellValue("Máscara WAN");
        header.createCell(7).setCellValue("WAN+1");
        header.createCell(8).setCellValue("WAN+2");
        header.createCell(9).setCellValue("WAN Broadcast");
        header.createCell(10).setCellValue("Es histórico");
        header.createCell(11).setCellValue("Tiene conflicto");
        header.createCell(12).setCellValue("Detalle conflicto");
        header.createCell(13).setCellValue("Acepta traslape");
        header.createCell(14).setCellValue("Justificación");
        header.createCell(15).setCellValue("Traslape aceptado por");
        header.createCell(16).setCellValue("Fecha aceptación");
        header.createCell(17).setCellValue("Último usuario");
        header.createCell(18).setCellValue("Última fecha");

        int rowNum = 1;
        for (Network n : networks) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(value(n.getDisplayIndex()));
            row.createCell(1).setCellValue(value(n.getNombreLugar()));
            row.createCell(2).setCellValue(value(n.getEnlace()));
            row.createCell(3).setCellValue(value(n.getNetworkIp()));
            row.createCell(4).setCellValue(value(n.getCidr()));
            row.createCell(5).setCellValue(value(n.getWanIp()));
            row.createCell(6).setCellValue(value(n.getWanCidr()));
            row.createCell(7).setCellValue(value(n.getWanIp1()));
            row.createCell(8).setCellValue(value(n.getWanIp2()));
            row.createCell(9).setCellValue(value(n.getWanBroadcast()));
            row.createCell(10).setCellValue(Boolean.TRUE.equals(n.getEsHistorico()) ? "Sí" : "No");
            row.createCell(11).setCellValue(Boolean.TRUE.equals(n.getTieneConflicto()) ? "Sí" : "No");
            row.createCell(12).setCellValue(value(n.getDetalleConflicto()));
            row.createCell(13).setCellValue(Boolean.TRUE.equals(n.getAceptaTraslape()) ? "Sí" : "No");
            row.createCell(14).setCellValue(value(n.getMotivoJustificacion()));
            row.createCell(15).setCellValue(value(n.getTraslapeAceptadoPor()));
            row.createCell(16).setCellValue(value(n.getTraslapeAceptadoAt()));
            row.createCell(17).setCellValue(value(n.getUpdatedBy()));
            row.createCell(18).setCellValue(value(n.getUpdatedAt()));
        }

        for (int i = 0; i <= 18; i++) {
            sheet.autoSizeColumn(i);
        }

        workbook.write(response.getOutputStream());
        workbook.close();
    }

        @GetMapping("/export/pdf")
    public void exportPdf(HttpServletResponse response) throws Exception {
        List<Network> networks = ipService.findAllOrdered();

        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=redes.pdf");

        Document document = new Document(PageSize.A4.rotate(), 20, 20, 45, 30);
        PdfWriter.getInstance(document, response.getOutputStream());

        document.open();

        var titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11);
        var normalFont = FontFactory.getFont(FontFactory.HELVETICA, 8);

        Phrase headerPhrase = new Phrase(
                "Sistema de Asignación de IP CCSS - Reporte de Redes",
                titleFont
        );
        HeaderFooter header = new HeaderFooter(headerPhrase, false);
        header.setAlignment(Element.ALIGN_CENTER);
        header.setBorder(0);
        document.setHeader(header);

        Phrase footerPhrase = new Phrase("Página ", normalFont);
        HeaderFooter footer = new HeaderFooter(footerPhrase, true);
        footer.setAlignment(Element.ALIGN_RIGHT);
        footer.setBorder(0);
        document.setFooter(footer);

        Paragraph intro = new Paragraph("Listado general de redes registradas", titleFont);
        intro.setSpacingAfter(10);
        document.add(intro);

        PdfPTable table = new PdfPTable(19);
        table.setWidthPercentage(100);
        table.setHeaderRows(1);

        addHeaderCell(table, "ID");
        addHeaderCell(table, "Lugar");
        addHeaderCell(table, "Enlace");
        addHeaderCell(table, "LAN IP");
        addHeaderCell(table, "Máscara LAN");
        addHeaderCell(table, "WAN IP");
        addHeaderCell(table, "Máscara WAN");
        addHeaderCell(table, "WAN+1");
        addHeaderCell(table, "WAN+2");
        addHeaderCell(table, "WAN Broadcast");
        addHeaderCell(table, "Histórico");
        addHeaderCell(table, "Conflicto");
        addHeaderCell(table, "Detalle");
        addHeaderCell(table, "Acepta traslape");
        addHeaderCell(table, "Justificación");
        addHeaderCell(table, "Aceptado por");
        addHeaderCell(table, "Fecha aceptación");
        addHeaderCell(table, "Último usuario");
        addHeaderCell(table, "Última fecha");

        for (Network n : networks) {
            table.addCell(new Phrase(value(n.getDisplayIndex()), normalFont));
            table.addCell(new Phrase(value(n.getNombreLugar()), normalFont));
            table.addCell(new Phrase(value(n.getEnlace()), normalFont));
            table.addCell(new Phrase(value(n.getNetworkIp()), normalFont));
            table.addCell(new Phrase(value(n.getCidr()), normalFont));
            table.addCell(new Phrase(value(n.getWanIp()), normalFont));
            table.addCell(new Phrase(value(n.getWanCidr()), normalFont));
            table.addCell(new Phrase(value(n.getWanIp1()), normalFont));
            table.addCell(new Phrase(value(n.getWanIp2()), normalFont));
            table.addCell(new Phrase(value(n.getWanBroadcast()), normalFont));
            table.addCell(new Phrase(Boolean.TRUE.equals(n.getEsHistorico()) ? "Sí" : "No", normalFont));
            table.addCell(new Phrase(Boolean.TRUE.equals(n.getTieneConflicto()) ? "Sí" : "No", normalFont));
            table.addCell(new Phrase(value(n.getDetalleConflicto()), normalFont));
            table.addCell(new Phrase(Boolean.TRUE.equals(n.getAceptaTraslape()) ? "Sí" : "No", normalFont));
            table.addCell(new Phrase(value(n.getMotivoJustificacion()), normalFont));
            table.addCell(new Phrase(value(n.getTraslapeAceptadoPor()), normalFont));
            table.addCell(new Phrase(value(n.getTraslapeAceptadoAt()), normalFont));
            table.addCell(new Phrase(value(n.getUpdatedBy()), normalFont));
            table.addCell(new Phrase(value(n.getUpdatedAt()), normalFont));
        }

        document.add(table);
        document.close();
    }

    private void addHeaderCell(PdfPTable table, String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text));
        table.addCell(cell);
    }

    private String value(Object value) {
        return value == null ? "" : value.toString();
    }

    private String csv(Object value) {
        String text = value == null ? "" : value.toString();
        text = text.replace("\"", "\"\"");
        return "\"" + text + "\"";
    }
}
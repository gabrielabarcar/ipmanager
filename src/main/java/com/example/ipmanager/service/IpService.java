package com.example.ipmanager.service;

import com.example.ipmanager.model.Network;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public interface IpService {

    List<Network> findAllOrdered();

    Network save(Network network);

    void deleteById(Long id);

    List<Network> importCsvEstricto(MultipartFile file) throws IOException;

    List<Network> importCsvHistorico(MultipartFile file) throws IOException;

    Map<String, Object> validateEnlace(String enlace, Long id);

    Map<String, Object> validateLan(String ip, Integer cidr, Long id);

    Map<String, Object> validateWan(String ip, Integer cidr, Long id);

    String obtenerSiguienteIpDisponible(String tipo);
}
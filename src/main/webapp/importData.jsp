<%@page contentType="application/json" pageEncoding="UTF-8"%>
<%@page import="java.nio.file.Files"%>
<%@page import="java.io.File"%>
<%
    String webPath = application.getRealPath("/");
    File dataFile = new File(webPath, "../../data/medicaltest.json");
    if (!dataFile.exists()) {
        dataFile = new File("C:/Users/songn/Downloads/Project1/project/FIX3/data/medicaltest.json");
    }
    
    try {
        String content = new String(Files.readAllBytes(dataFile.toPath()), "UTF-8");
        out.print(content);
    } catch (Exception e) {
        response.setStatus(500);
        out.print("{\"error\": \"Could not read data file\"}");
    }
%>

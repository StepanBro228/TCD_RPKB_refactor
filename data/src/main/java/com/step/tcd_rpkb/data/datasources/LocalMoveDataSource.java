package com.step.tcd_rpkb.data.datasources;

import android.content.Context;
import android.content.res.AssetManager;
import com.google.gson.Gson;
import com.step.tcd_rpkb.data.mapper.InvoiceMapper;
import com.step.tcd_rpkb.data.mapper.MoveResponseMapper;
import com.step.tcd_rpkb.data.network.dto.InvoiceDto;
import com.step.tcd_rpkb.data.network.dto.MoveResponseDto;
import com.step.tcd_rpkb.domain.model.Invoice;
import com.step.tcd_rpkb.domain.model.MoveResponse;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import javax.inject.Inject;
import dagger.hilt.android.qualifiers.ApplicationContext;

public class LocalMoveDataSource {

    private final Context appContext;
    private final Gson gson;
    private final InvoiceMapper invoiceMapper;
    private final MoveResponseMapper moveResponseMapper;

    @Inject
    public LocalMoveDataSource(@ApplicationContext Context appContext, 
                               Gson gson, 
                               InvoiceMapper invoiceMapper, 
                               MoveResponseMapper moveResponseMapper) {
        this.appContext = appContext;
        this.gson = gson;
        this.invoiceMapper = invoiceMapper;
        this.moveResponseMapper = moveResponseMapper;
    }

    public MoveResponse getMoveList() throws IOException {
        String jsonString = loadJSONFromAssets("moves.json");
        MoveResponseDto dto = gson.fromJson(jsonString, MoveResponseDto.class);
        if (dto == null) {
            throw new IOException("Не удалось распарсить moves.json или файл пуст.");
        }
        return moveResponseMapper.mapToDomain(dto);
    }

    public Invoice getDocumentMove(String guid) throws IOException {
        String fileName = getFileNameForGuid(guid);
        if (fileName == null) {
            throw new IOException("Для GUID " + guid + " нет соответствующего локального файла.");
        }

        // Логика копирования файла из assets, если это все еще необходимо.
        // В оригинальном DataProvider файл копировался для чтения через FileReader.
        // Если Gson может работать напрямую с InputStream из AssetManager, это может быть проще.
        // Пока оставим логику копирования, аналогичную DataProvider, для консистентности.
        File jsonFile = copyAssetToFile(fileName);
        
        StringBuilder jsonContent = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(jsonFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                jsonContent.append(line);
            }
        }
        
        InvoiceDto dto = gson.fromJson(jsonContent.toString(), InvoiceDto.class);
        if (dto == null) {
            throw new IOException("Не удалось распарсить " + fileName + " или файл пуст.");
        }
        return invoiceMapper.mapToDomain(dto);
    }

    private String getFileNameForGuid(String guid) {
        // Логика из DataProvider.loadInvoiceFromLocalFile
        if ("2d7f7704-f268-11ee-bba5-001dd8b71c23".equals(guid)) {
            return "1move.json";
        } else if ("e4fe17c4-d722-11ef-bbad-001dd8b71c23".equals(guid)) {
            return "2move.json";
        }
        return null;
    }

    private String loadJSONFromAssets(String fileName) throws IOException {
        AssetManager assetManager = appContext.getAssets();
        try (InputStream is = assetManager.open(fileName);
             InputStreamReader inputStreamReader = new InputStreamReader(is, StandardCharsets.UTF_8);
             BufferedReader reader = new BufferedReader(inputStreamReader)) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            return sb.toString();
        }
    }

    private File copyAssetToFile(String assetName) throws IOException {
        File tempFile = new File(appContext.getCacheDir(), "temp_" + assetName);
        if (tempFile.exists()) {
            // Можно добавить логику проверки актуальности файла или просто перезаписывать
            // tempFile.delete(); 
        }
        // Если файл не существует, или мы решили его перезаписать
        if (!tempFile.exists()) { 
            try (InputStream in = appContext.getAssets().open(assetName);
                 FileOutputStream out = new FileOutputStream(tempFile)) {
                byte[] buffer = new byte[1024];
                int read;
                while ((read = in.read(buffer)) != -1) {
                    out.write(buffer, 0, read);
                }
            }
        }
        return tempFile;
    }
} 
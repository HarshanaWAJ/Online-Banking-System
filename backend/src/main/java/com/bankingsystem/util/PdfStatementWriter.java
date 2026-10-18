package com.bankingsystem.util;

import com.bankingsystem.entity.BankAccount;
import com.bankingsystem.entity.LedgerEntry;
import com.bankingsystem.enums.EntryDirection;
import com.lowagie.text.Document;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Statement PDF generator using OpenPDF.
 */
public final class PdfStatementWriter {

  private static final DateTimeFormatter DATE_FORMAT =
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.of("UTC"));

  private PdfStatementWriter() {}

  public static byte[] generateStatementPdf(
      BankAccount account,
      List<LedgerEntry> entries,
      BigDecimal openingBalance,
      BigDecimal closingBalance,
      Instant from,
      Instant to) {

    try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      Document document = new Document();
      PdfWriter.getInstance(document, out);
      document.open();

      // Header
      Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, Color.DARK_GRAY);
      Paragraph title = new Paragraph("ACCOUNT STATEMENT", titleFont);
      title.setAlignment(Paragraph.ALIGN_CENTER);
      title.setSpacingAfter(15);
      document.add(title);

      // Account Info Table
      Font regularFont = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.BLACK);
      Font boldFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.BLACK);

      PdfPTable infoTable = new PdfPTable(2);
      infoTable.setWidthPercentage(100);
      infoTable.setSpacingAfter(15);

      addInfoRow(infoTable, "Account Holder:", account.getCustomer().getFullName(), boldFont, regularFont);
      addInfoRow(infoTable, "Account Number:", account.getAccountNumber(), boldFont, regularFont);
      addInfoRow(infoTable, "Account Type:", account.getType().name(), boldFont, regularFont);
      addInfoRow(infoTable, "Currency:", account.getCurrency(), boldFont, regularFont);
      addInfoRow(infoTable, "Opening Balance:", "LKR " + openingBalance.toPlainString(), boldFont, regularFont);
      addInfoRow(infoTable, "Closing Balance:", "LKR " + closingBalance.toPlainString(), boldFont, regularFont);
      document.add(infoTable);

      // Entries Table
      PdfPTable table = new PdfPTable(5);
      table.setWidthPercentage(100);
      table.setWidths(new float[] {2.5f, 2.5f, 1.5f, 1.8f, 1.8f});
      table.setHeaderRows(1);

      // Header row
      String[] headers = {"Date (UTC)", "Reference / Details", "Type", "Amount", "Balance After"};
      for (String header : headers) {
        PdfPCell cell = new PdfPCell(new Phrase(header, boldFont));
        cell.setBackgroundColor(new Color(230, 235, 245));
        cell.setPadding(6);
        table.addCell(cell);
      }

      // Data rows
      for (LedgerEntry entry : entries) {
        table.addCell(createCell(DATE_FORMAT.format(entry.getCreatedAt()), regularFont));
        table.addCell(createCell(entry.getReference() + "\n" + entry.getDescription(), regularFont));
        table.addCell(createCell(entry.getDirection().name(), regularFont));

        String amountStr = (entry.getDirection() == EntryDirection.DEBIT ? "-" : "+") + entry.getAmount().toPlainString();
        PdfPCell amountCell = createCell(amountStr, regularFont);
        if (entry.getDirection() == EntryDirection.DEBIT) {
          amountCell.setPhrase(new Phrase(amountStr, FontFactory.getFont(FontFactory.HELVETICA, 10, Color.RED)));
        } else {
          amountCell.setPhrase(new Phrase(amountStr, FontFactory.getFont(FontFactory.HELVETICA, 10, new Color(0, 128, 0))));
        }
        table.addCell(amountCell);

        table.addCell(createCell("LKR " + entry.getBalanceAfter().toPlainString(), regularFont));
      }

      document.add(table);
      document.close();
      return out.toByteArray();
    } catch (Exception ex) {
      throw new RuntimeException("Failed to generate PDF statement: " + ex.getMessage(), ex);
    }
  }

  private static void addInfoRow(PdfPTable table, String label, String value, Font labelFont, Font valFont) {
    PdfPCell c1 = new PdfPCell(new Phrase(label, labelFont));
    c1.setBorder(Rectangle.NO_BORDER);
    c1.setPadding(3);
    PdfPCell c2 = new PdfPCell(new Phrase(value, valFont));
    c2.setBorder(Rectangle.NO_BORDER);
    c2.setPadding(3);
    table.addCell(c1);
    table.addCell(c2);
  }

  private static PdfPCell createCell(String text, Font font) {
    PdfPCell cell = new PdfPCell(new Phrase(text, font));
    cell.setPadding(5);
    return cell;
  }
}

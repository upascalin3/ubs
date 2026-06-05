package com.utility.billing.billing.service;

import com.utility.billing.auth.entity.User;
import com.utility.billing.billing.entity.Bill;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
public class BillPdfService {

	private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

	public byte[] generate(Bill bill, User user) {
		List<String> lines = List.of(
				"UTILITY BILLING SYSTEM",
				"Bill Number: " + bill.getBillNumber(),
				"Customer: " + user.getFullName(),
				"Email: " + user.getEmail(),
				"Meter ID: " + bill.getMeterId(),
				"Billing Period: " + bill.getBillingMonth() + "/" + bill.getBillingYear(),
				"Consumption: " + bill.getConsumption(),
				"Subtotal: " + bill.getAmount() + " RWF",
				"VAT/Tax: " + bill.getTaxAmount() + " RWF",
				"Penalty: " + bill.getPenalty() + " RWF",
				"Balance Due: " + bill.getBalance() + " RWF",
				"Status: " + bill.getStatus(),
				"Generated: " + bill.getGeneratedDate().format(DATE_TIME));

		StringBuilder stream = new StringBuilder();
		stream.append("BT\n/F1 18 Tf\n50 780 Td\n(UTILITY BILL) Tj\n");
		stream.append("/F1 11 Tf\n0 -32 Td\n");
		for (String line : lines) {
			stream.append("(").append(escape(line)).append(") Tj\n0 -18 Td\n");
		}
		stream.append("ET\n");
		byte[] content = stream.toString().getBytes(StandardCharsets.US_ASCII);

		List<byte[]> objects = new ArrayList<>();
		objects.add("1 0 obj\n<< /Type /Catalog /Pages 2 0 R >>\nendobj\n".getBytes(StandardCharsets.US_ASCII));
		objects.add("2 0 obj\n<< /Type /Pages /Kids [3 0 R] /Count 1 >>\nendobj\n".getBytes(StandardCharsets.US_ASCII));
		objects.add("3 0 obj\n<< /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] /Resources << /Font << /F1 4 0 R >> >> /Contents 5 0 R >>\nendobj\n".getBytes(StandardCharsets.US_ASCII));
		objects.add("4 0 obj\n<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>\nendobj\n".getBytes(StandardCharsets.US_ASCII));
		objects.add(("5 0 obj\n<< /Length " + content.length + " >>\nstream\n"
				+ new String(content, StandardCharsets.US_ASCII) + "endstream\nendobj\n").getBytes(StandardCharsets.US_ASCII));

		return writePdf(objects);
	}

	private byte[] writePdf(List<byte[]> objects) {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		List<Integer> offsets = new ArrayList<>();
		write(out, "%PDF-1.4\n");
		for (byte[] object : objects) {
			offsets.add(out.size());
			out.writeBytes(object);
		}
		int xrefStart = out.size();
		write(out, "xref\n0 " + (objects.size() + 1) + "\n");
		write(out, "0000000000 65535 f \n");
		for (Integer offset : offsets) {
			write(out, String.format("%010d 00000 n \n", offset));
		}
		write(out, "trailer\n<< /Size " + (objects.size() + 1) + " /Root 1 0 R >>\n");
		write(out, "startxref\n" + xrefStart + "\n%%EOF\n");
		return out.toByteArray();
	}

	private void write(ByteArrayOutputStream out, String value) {
		out.writeBytes(value.getBytes(StandardCharsets.US_ASCII));
	}

	private String escape(String value) {
		return value.replace("\\", "\\\\")
				.replace("(", "\\(")
				.replace(")", "\\)");
	}
}

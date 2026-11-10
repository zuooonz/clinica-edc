/*
 * Clinica EDC synthetic certificate regression test.
 * SPDX-License-Identifier: LGPL-2.1-or-later
 */
package org.akaza.openclinica.service.otp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.akaza.openclinica.dao.core.CoreResources;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.graphics.PDXObject;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.Test;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.Result;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;

/** Exercises the real service using a fixed public synthetic secret; all PDF bytes stay in memory. */
public class TwoFactorCertificateTest {
    @Test
    public void certificatePreservesAccountTextAndDecodableTotpQr() throws Exception {
        String syntheticSecret = "JBSWY3DPEHPK3PXP";
        Properties settings = new Properties();
        settings.setProperty("sysURL", "https://edc.example.invalid/LibreClinica/MainMenu");
        CoreResources resources = mock(CoreResources.class);
        when(resources.getDATAINFO()).thenReturn(settings);
        TwoFactorService service = new TwoFactorService();
        service.setCoreResources(resources);
        CertificateBean bean = new CertificateBean();
        bean.setUsername("Synthetic User");
        bean.setLogin("synthetic.user");
        bean.setEmail("synthetic@example.invalid");
        bean.setSecret(syntheticSecret);

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        service.printoutCertificate(bean, output);
        try (PDDocument pdf = PDDocument.load(output.toByteArray())) {
            String text = new PDFTextStripper().getText(pdf).replaceAll("\\s+", " ");
            assertTrue("Certificate heading", text.contains("2-Factor Authentication Certificate"));
            assertTrue("Synthetic account name", text.contains("User: Synthetic User"));
            assertTrue("Synthetic login", text.contains("Login: synthetic.user"));
            assertTrue("Synthetic email", text.contains("E-Mail: synthetic@example.invalid"));
            assertTrue("Grouped synthetic secret survives rendering", text.contains("JBSW Y3DP EHPK 3PXP"));
            int qrImages = 0;
            for (PDPage page : pdf.getPages()) {
                for (COSName name : page.getResources().getXObjectNames()) {
                    PDXObject object = page.getResources().getXObject(name);
                    if (object instanceof PDImageXObject) {
                        qrImages++;
                        BufferedImage image = ((PDImageXObject) object).getImage();
                        BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(new BufferedImageLuminanceSource(image)));
                        Result result = new MultiFormatReader().decode(bitmap);
                        assertEquals(BarcodeFormat.QR_CODE, result.getBarcodeFormat());
                        URI otp = URI.create(result.getText());
                        assertEquals("otpauth", otp.getScheme());
                        assertEquals("totp", otp.getHost());
                        assertEquals("/LibreClinica (edc.example.invalid/LibreClinica)", otp.getPath());
                        Map<String, String> query = new HashMap<String, String>();
                        for (String field : otp.getRawQuery().split("&")) {
                            String[] pair = field.split("=", 2);
                            query.put(URLDecoder.decode(pair[0], "UTF-8"), URLDecoder.decode(pair[1], "UTF-8"));
                        }
                        // Do not include even synthetic secret values in assertion failure output.
                        assertTrue("QR secret matches the synthetic certificate", syntheticSecret.equals(query.get("secret")));
                        assertEquals("LibreClinica", query.get("issuer"));
                        assertEquals("SHA1", query.get("algorithm"));
                        assertEquals("6", query.get("digits"));
                        assertEquals("30", query.get("period"));
                    }
                }
            }
            assertEquals("One decodable QR code is embedded", 1, qrImages);
        }
    }
}

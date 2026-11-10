/* Clinica source-build regression tests. SPDX-License-Identifier: MIT */
package org.clinica.vendor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.graphics.PDXObject;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;
import org.junit.Test;

import rst.pdfbox.layout.elements.Document;
import rst.pdfbox.layout.elements.ImageElement;
import rst.pdfbox.layout.elements.PageFormat;
import rst.pdfbox.layout.elements.Paragraph;
import rst.pdfbox.layout.elements.PositionControl;

/** Reads produced PDF bytes back through PDFBox; no reference binaries or user data. */
public class PdfBoxLayoutTest {
    private static byte[] save(Document document) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        document.save(output);
        return output.toByteArray();
    }

    @Test
    public void wrappingPreservesTextAndUsesSeveralLines() throws Exception {
        String source = "ALPHA BETA GAMMA DELTA EPSILON ZETA ETA THETA IOTA KAPPA END";
        Document document = new Document(40, 40, 40, 40);
        Paragraph paragraph = new Paragraph();
        paragraph.setMaxWidth(110);
        paragraph.addText(source, 12, PDType1Font.COURIER);
        document.add(paragraph);
        try (PDDocument pdf = PDDocument.load(save(document))) {
            Positions stripper = new Positions();
            assertEquals(source, stripper.getText(pdf).trim().replaceAll("\\s+", " "));
            Set<Integer> lines = new HashSet<Integer>();
            for (TextPosition position : stripper.positions) {
                lines.add(Math.round(position.getYDirAdj()));
                assertTrue("Wrapped text remains inside its width", position.getXDirAdj() < 151);
            }
            assertTrue("Expected actual PDF text on several lines", lines.size() > 1);
        }
    }

    @Test
    public void longParagraphPaginatesWithoutDroppingOrReorderingText() throws Exception {
        Document document = new Document(PageFormat.with().mediaBox(new PDRectangle(240, 200))
                .margins(20, 20, 20, 20).build());
        Paragraph paragraph = new Paragraph();
        paragraph.setMaxWidth(180);
        for (int i = 0; i < 80; i++) {
            paragraph.addText(String.format("ROW_%03d synthetic text\n", i), 10, PDType1Font.COURIER);
        }
        document.add(paragraph);
        try (PDDocument pdf = PDDocument.load(save(document))) {
            assertTrue("Expected more than one PDF page", pdf.getNumberOfPages() > 1);
            String text = new PDFTextStripper().getText(pdf);
            int previous = -1;
            for (int i = 0; i < 80; i++) {
                String marker = String.format("ROW_%03d", i);
                int found = text.indexOf(marker);
                assertTrue("All row markers survive pagination in order", found > previous);
                assertEquals("Each row marker appears once", found, text.lastIndexOf(marker));
                previous = found;
            }
        }
    }

    @Test
    public void relativePositionMovesPdfTextAndImagePixelsSurviveEmbedding() throws Exception {
        List<TextPosition> original = markerPositions(0, 0);
        List<TextPosition> moved = markerPositions(30, -20);
        assertEquals(original.get(0).getXDirAdj() + 30, moved.get(0).getXDirAdj(), 0.1);
        assertEquals(original.get(0).getYDirAdj() + 20, moved.get(0).getYDirAdj(), 0.1);

        BufferedImage source = new BufferedImage(32, 24, BufferedImage.TYPE_INT_RGB);
        for (int x = 0; x < source.getWidth(); x++) {
            for (int y = 0; y < source.getHeight(); y++) {
                source.setRGB(x, y, (x + y) % 2 == 0 ? 0x00FFFFFF : 0x00000000);
            }
        }
        Document document = new Document(40, 40, 40, 40);
        ImageElement image = new ImageElement(source);
        image.setWidth(96);
        image.setHeight(72);
        document.add(image);
        try (PDDocument pdf = PDDocument.load(save(document))) {
            int images = 0;
            for (PDPage page : pdf.getPages()) {
                for (COSName name : page.getResources().getXObjectNames()) {
                    PDXObject object = page.getResources().getXObject(name);
                    if (object instanceof PDImageXObject) {
                        images++;
                        BufferedImage decoded = ((PDImageXObject) object).getImage();
                        assertEquals(source.getWidth(), decoded.getWidth());
                        assertEquals(source.getHeight(), decoded.getHeight());
                        for (int x = 0; x < source.getWidth(); x++) {
                            for (int y = 0; y < source.getHeight(); y++) {
                                assertEquals("Lossless image pixels", source.getRGB(x, y), decoded.getRGB(x, y));
                            }
                        }
                    }
                }
            }
            assertEquals("Exactly one embedded image", 1, images);
        }
    }

    private static List<TextPosition> markerPositions(float x, float y) throws IOException {
        Document document = new Document(40, 40, 40, 40);
        document.add(PositionControl.createMovePosition(x, y));
        Paragraph paragraph = new Paragraph();
        paragraph.addText("POSITION_MARKER", 12, PDType1Font.COURIER);
        document.add(paragraph);
        try (PDDocument pdf = PDDocument.load(save(document))) {
            Positions stripper = new Positions();
            assertTrue(stripper.getText(pdf).contains("POSITION_MARKER"));
            return stripper.positions;
        }
    }

    private static class Positions extends PDFTextStripper {
        private final List<TextPosition> positions = new ArrayList<TextPosition>();

        Positions() throws IOException {
            super();
        }

        @Override
        protected void writeString(String text, List<TextPosition> textPositions) throws IOException {
            positions.addAll(textPositions);
            super.writeString(text, textPositions);
        }
    }
}

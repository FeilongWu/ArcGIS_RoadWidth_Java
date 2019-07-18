package net.mapsay.polygon;

import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.junit.Test;

/**
 * @author 卞河儒
 * @email bianheru@mapsay.net
 * @desc (描述)
 * @date 2019-05-15
 **/
public class XmlTest {

    @Test
    public void testCreateDocument() {
        Document document = DocumentHelper.createDocument();
        Element root = document.addElement("root");

        root.addElement("author")
                .addAttribute("name", "James")
                .addAttribute("location", "UK")
                .addText("James Strachan");

        root.addElement("author")
                .addAttribute("name", "Bob")
                .addAttribute("location", "US")
                .addText("Bob McWhirter");

        System.out.println(document.asXML());
    }
}

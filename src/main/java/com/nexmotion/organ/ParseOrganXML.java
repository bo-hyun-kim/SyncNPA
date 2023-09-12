package com.nexmotion.organ;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import com.nexmotion.account.Account;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.nexmotion.xmlUtil.XmlUtil;
import org.xml.sax.InputSource;

@Component
public class ParseOrganXML {

  @Autowired
  private OrganService organService;

	public void parseOrganData(String parameter) {

		try {
			String rData = parameter;
			System.err.println("organdata===>" + rData);

			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = factory.newDocumentBuilder();
			Document doc = builder.parse(new InputSource(new StringReader(rData)));
			Element rootElement = doc.getDocumentElement();

			String respCd = rootElement.getElementsByTagName("RESP_CD").item(0).getTextContent();

			List<Organ> existingData = organService.selectOrgan();

			// 00 : 응답데이터 있고 추가 데이터 없는 경우
			// 01 : 응답 데이터가 아예 없는 경우
			// 02 : 응답데이터 있고 추가 데이터 있는 경우
			// 그 외 : 에러인 경우

			if (respCd.equals("01")) {
				return;
			}

			// 에러 향후 재처리
			if (!respCd.equals("02") && !respCd.equals("00")) {
				String errorMessage = String.format("%s", respCd.toString());
				throw new Exception(errorMessage);
			}

			List<Organ> newData = parseData(doc);

			if (existingData.size() == 0) {
				organService.insertOrganList(newData);
			} else {
				compareData(newData, existingData);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public List<Organ> parseData(Document doc) throws Exception {
		List<Organ> organList = new ArrayList<>();

		try {
			Element responseElement = doc.getDocumentElement();

			Element headerElement = (Element) responseElement.getElementsByTagName("HEADER").item(0);
			String tlgrCd = getElementText(headerElement, "TLGR_CD");
			String respCd = getElementText(headerElement, "RESP_CD");

			Element dataElement = (Element) responseElement.getElementsByTagName("DATA").item(0);
			NodeList recordList = dataElement.getElementsByTagName("RECORD");

			for (Node recordNode : asList(recordList)) {
				Organ organ = new Organ();
				Element recordElement = (Element) recordNode;
				String organGvofcode = getElementText(recordElement, "GVOF_CD");
				String organGvofname = getElementText(recordElement, "GVOF_NM");
				String organHgvofcode = getElementText(recordElement, "HGHR_GVOF_CD");
				String organUseyn = getElementText(recordElement, "USE_YN");
				String chgdate = getElementText(recordElement, "CHG_DTTM");

				organ.setGvofcode(organGvofcode);
				organ.setGvofname(organGvofname);
				organ.setHgvofcode(organHgvofcode);
				organ.setUseyn(organUseyn);
				organ.setChgdate(chgdate);

				organList.add(organ);
				System.err.println("organ===>"+organ);

			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return organList;
	}

	private static String getElementText(Element parentElement, String tagName) {
		NodeList nodeList = parentElement.getElementsByTagName(tagName);
		if (nodeList.getLength() > 0) {
			Node node = nodeList.item(0);
			return node.getTextContent();
		}
		return null;
	}

	private static List<Node> asList(NodeList nodeList) {
		List<Node> nodeListAsList = new ArrayList<>();
		for (int i = 0; i < nodeList.getLength(); i++) {
			nodeListAsList.add(nodeList.item(i));
		}
		return nodeListAsList;
	}

	private void compareData(List<Organ> newData, List<Organ> existingData) throws Exception {
		for (Organ newOrgan : newData) {
			boolean found = false;
			for (Organ existingOrgan : existingData) {
				if (newOrgan.getGvofcode().equals(existingOrgan.getGvofcode())) {
					System.err.println("겹치는 데이터" + newData);
					organService.updateOrgan(newOrgan);
					found = true;
					break;
				}
			}
			if (!found) {
				organService.insertOrgan(newOrgan);
			}
		}
	}

}
package life.qbic.portlet.io;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.util.StreamReaderDelegate;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * communicates with ncbi taxonomy db to find species by their Scientific Name or taxonomy ID
 * 
 * @author Andreas Friedrich
 *
 */
public class TaxonomySearcher {

  private final Map<String, TissueClass> kingdomToCategory = new HashMap<String, TissueClass>() {
    {
      put("Viridiplantae", TissueClass.Plants);
      put("Metazoa", TissueClass.Animals);
      put("Fungi", TissueClass.Fungi);
    }
  };

  final private String ncbiTaxRestURL =
      "https://eutils.ncbi.nlm.nih.gov/entrez/eutils/efetch.fcgi?db=taxonomy&id=";
  final private String ncbiSearchRestURL =
      "https://eutils.ncbi.nlm.nih.gov/entrez/eutils/esearch.fcgi?db=taxonomy&term=";


  private static String getScientificNameFromXML(InputStream stream)
      throws XPathExpressionException, SAXException, IOException, ParserConfigurationException {
    DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
    DocumentBuilder builder = builderFactory.newDocumentBuilder();
    Document xmlDocument = builder.parse(stream);
    XPath xPath = XPathFactory.newInstance().newXPath();

    String expression = "/TaxaSet/Taxon/ScientificName";
    NodeList nodeList =
        (NodeList) xPath.compile(expression).evaluate(xmlDocument, XPathConstants.NODESET);

    String name = null;
    for (int i = 0; i < nodeList.getLength(); i++) {
      name = nodeList.item(i).getFirstChild().getNodeValue();
    }
    return name;
  }

  public List<NCBITerm> getSpecies(String search)
      throws IOException, XPathExpressionException, SAXException, ParserConfigurationException {
    List<String> candidates = new ArrayList<String>();
    candidates.add(search);
    try {
      Integer.parseInt(search);
    } catch (NumberFormatException e) {
      candidates = getTaxIDsByName(search);
    }
    List<NCBITerm> res = new ArrayList<NCBITerm>();
    for (String id : candidates)
      res.add(searchByID(id));
//    Collections.sort(res);
    return res;
  }

  private List<String> getTaxIDsByName(String search)
      throws IOException, XPathExpressionException, SAXException, ParserConfigurationException {
    search = URLEncoder.encode(search, "UTF-8").replace("+", "%20");

    InputStream stream =
        httpCall(ncbiSearchRestURL.concat(search).concat("[subtree]").concat("&retmode=xml"));

    List<String> taxIDs = searchTaxIDsInXML(stream);

    return taxIDs;
  }

  private List<String> searchTaxIDsInXML(InputStream stream)
      throws XPathExpressionException, SAXException, IOException, ParserConfigurationException {
    DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
    DocumentBuilder builder = builderFactory.newDocumentBuilder();
    Document xmlDocument = builder.parse(stream);
    XPath xPath = XPathFactory.newInstance().newXPath();

    String expression = "/eSearchResult/IdList/Id";
    NodeList nodeList =
        (NodeList) xPath.compile(expression).evaluate(xmlDocument, XPathConstants.NODESET);

    List<String> taxIDs = new ArrayList<String>();
    for (int i = 0; i < nodeList.getLength(); i++) {
      taxIDs.add(nodeList.item(i).getFirstChild().getNodeValue());
    }
    return taxIDs;
  }

  private NCBITerm searchByID(String id)
      throws IOException, XPathExpressionException, SAXException, ParserConfigurationException {
    InputStream stream = httpCall(ncbiTaxRestURL.concat(id).concat("&retmode=xml"));

    String name = getScientificNameFromXML(stream);

    return new NCBITerm(id, name, "");
  }

  public static InputStream httpCall(String url) throws IOException {
    final HttpURLConnection connection = (HttpURLConnection) (new URL(url).openConnection());
    connection.setRequestMethod("GET");
    connection.connect();
    return connection.getInputStream();
  }

  public String getKingdomTerm(String taxID)
      throws IOException, ParserConfigurationException, SAXException, XPathExpressionException {
    InputStream stream =
        httpCall("https://eutils.ncbi.nlm.nih.gov/entrez/eutils/efetch.fcgi?db=taxonomy&id="
            .concat(taxID).concat("&retmode=xml"));
    DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
    DocumentBuilder builder = builderFactory.newDocumentBuilder();
    Document xmlDocument = builder.parse(stream);
    XPath xPath = XPathFactory.newInstance().newXPath();

    String expression = "/TaxaSet/Taxon/Lineage";
    NodeList nodeList =
        (NodeList) xPath.compile(expression).evaluate(xmlDocument, XPathConstants.NODESET);

    String lineage = nodeList.item(0).getFirstChild().getNodeValue();
    // TODO use xpath
    for (String key : kingdomToCategory.keySet()) {
      if (lineage.contains(key))
        return kingdomToCategory.get(key).toString();
    }
    return "unknown";
  }

  public static void main(String[] args)
      throws XPathExpressionException, SAXException, ParserConfigurationException, IOException {
    TaxonomySearcher t = new TaxonomySearcher();
    // String taxID = t.getSpecies("arabidopsis thaliana").get(0).getTaxID();
    // System.out.println(t.getKingdom(taxID));

    // InputStream stream =
    // httpCall("https://eutils.ncbi.nlm.nih.gov/entrez/eutils/efetch.fcgi?db=taxonomy&id="
    // .concat(taxID).concat("&retmode=xml"));
    // BufferedReader rd = new BufferedReader(new InputStreamReader(stream));
    //
    // String line;
    //
    // while ((line = rd.readLine()) != null) {
    //
    // System.out.println(line);
    //
    // }
    Set<String> x = new HashSet<String>(Arrays.asList("Foeniculum vulgare", "Levisticum officinale",
        "Astrantia major", "Carum carvi", "Petroselinum crispum", "Bupleurum handiense",
        "Bupleurum falcatum", "Athamanta cretensis", "Seseli libanotis", "Sanicula odorata",
        "Vinca major", "Artemisia abrotanum", "Artemisia absinthium", "Achillea millefolium",
        "Centaurea montana", "Carduus defloratus", "Echinacea purpurea", "Helichrysum arenarium",
        "Eupatorium cannabinum", "Tagetes erecta", "Calendula officinalis", "Erysimum cheiri",
        "Matthiola incana", "Brassica napus", "Aurinia saxatilis",
        "Brassica oleracea var. oleracea", "Bunias orientalis", "Iberis sempervirens",
        "Isatis tinctoria", "Descurainia millefolia", "Phlomis viscosa", "Mentha x piperita",
        "Teucrium chamaedrys", "Melissa officinalis", "Salvia officinalis", "Stachys byzantina",
        "Ocimum basilicum", "Ajuga reptans", "Leonurus cardiaca", "Lavandula canariensis",
        "Capsicum frutescens", "Nicotiana longiflora", "Nicotiana rustica", "Salpiglossis sinuata",
        "Physalis alkekengi", "Physalis peruviana", "Petunia axillaris", "Erysimum scoparium",
        "Coriandrum sativum", "Nerium oleander", "Asclepias syriaca", "Dahlia merckii",
        "Melittis melissophyllum", "Mandragora officinarum", "Capsicum annuum", "Hordeum vulgare",
        "Triticum aestivum", "Avena sativa", "Secale cereale"));
    for (String s : x)
      try {
        System.out.println(t.getSpecies(s));
      } catch (IOException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
  }

}

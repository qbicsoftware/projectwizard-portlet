package life.qbic.portlet;

import java.io.IOException;
import java.util.Properties;

import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.vaadin.annotations.Theme;
import com.vaadin.annotations.Widgetset;
import com.vaadin.server.VaadinRequest;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.Component;
import com.vaadin.ui.Label;
import com.vaadin.ui.Layout;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;

/**
 * This is the class from which all other QBiC portlets derive.
 */
@Theme("mytheme")
@SuppressWarnings("serial")
@Widgetset("life.qbic.portlet.AppWidgetSet")
public abstract class QBiCPortletUI extends UI {

    private final static Log LOG = LogFactoryUtil.getLog(QBiCPortletUI.class);
    // value replaced by cookiecutter
    private final static String PORTLET_REPOSITORY_URL = "http://github.com/qbicsoftware/projectwizard-portlet";
    private final static String PORTLET_VERSION;

    // load values from portlet.properties
    static {
        if (QBiCPortletUI.class.getClass() != Object.class.getClass()) {
            QBiCPortletUI.class.isAssignableFrom(Object.class);
        }

        final Properties portletProperties = new Properties();
        try {
            portletProperties.load(QBiCPortletUI.class.getClassLoader().getResourceAsStream("portlet.properties"));
        } catch (IOException e) {
            LOG.error("Error loading portlet.properties", e);
            throw new RuntimeException("Could not load portlet.properties file. Aborting portlet initialization.", e);
        }
        PORTLET_VERSION = portletProperties.getProperty("version");
    }
    
    @Override
    protected final void init(final VaadinRequest request) {
        LOG.info("Initializing portlet projectwizard-portlet, version " + PORTLET_VERSION);
        final VerticalLayout layout = new VerticalLayout();        
        layout.setMargin(true);
        // add the portlet
        layout.addComponent(getPortletContent(request));

        addPortletInfo(layout);
        setContent(layout);
        
    }

    @Override
    public final void setContent(Component content) {
        super.setContent(content);
    }

    // adds version and repository information to the passed layout
    private void addPortletInfo(final Layout layout) {
        final Label portletInfoLabel = 
        new Label("version " + PORTLET_VERSION + " (<a href=\"" + PORTLET_REPOSITORY_URL + "\">" + PORTLET_REPOSITORY_URL + "</a>)", ContentMode.HTML);
        portletInfoLabel.addStyleName("portlet-footer");
        layout.addComponent(portletInfoLabel);
    }

    /**
     * Provide the content that will be displayed.
     * 
     */
    protected abstract Layout getPortletContent(final VaadinRequest request);
}
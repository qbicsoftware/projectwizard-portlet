package life.qbic.portlet;

import org.junit.Test;

import life.qbic.portal.portlet.QBiCPortletUI;
import life.qbic.portal.portlet.ProjectWizardUI;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ProjectWizardUITest {

    @Test
    public void mainUIExtendsQBiCPortletUI() {
        assertTrue("The main UI class must extend life.qbic.projectwizard.QBiCPortletUI", 
            QBiCPortletUI.class.isAssignableFrom(ProjectWizardUI.class));
    }

    @Test
    public void mainUIIsNotQBiCPortletUI() {
        assertFalse("The main UI class must be different to life.qbic.projectwizard.QBiCPortletUI", 
            QBiCPortletUI.class.equals(ProjectWizardUI.class));
    }
}
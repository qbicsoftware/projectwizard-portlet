# -*- coding: utf-8 -*-
import ch.systemsx.cisd.openbis.generic.server.jython.api.v1.DataType as DataType

print ("Importing Master Data...")

tr = service.transaction()


prop_type_Q_EXPERIMENTAL_SETUP = tr.getOrCreateNewPropertyType('Q_EXPERIMENTAL_SETUP', DataType.XML)
prop_type_Q_EXPERIMENTAL_SETUP.setLabel('Experimental design and properties')
prop_type_Q_EXPERIMENTAL_SETUP.setManagedInternally(False)
prop_type_Q_EXPERIMENTAL_SETUP.setInternalNamespace(False)

assignment_EXPERIMENT_Q_PROJECT_DETAILS_Q_EXPERIMENTAL_SETUP = tr.assignPropertyType(exp_type_Q_PROJECT_DETAILS, prop_type_Q_EXPERIMENTAL_SETUP)
assignment_EXPERIMENT_Q_PROJECT_DETAILS_Q_EXPERIMENTAL_SETUP.setMandatory(False)
assignment_EXPERIMENT_Q_PROJECT_DETAILS_Q_EXPERIMENTAL_SETUP.setSection(None)
assignment_EXPERIMENT_Q_PROJECT_DETAILS_Q_EXPERIMENTAL_SETUP.setPositionInForms(3)
assignment_EXPERIMENT_Q_PROJECT_DETAILS_Q_EXPERIMENTAL_SETUP.setShownEdit(True)

print ("Import of Master Data finished.")
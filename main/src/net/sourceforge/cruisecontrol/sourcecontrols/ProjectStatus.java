package net.sourceforge.cruisecontrol.sourcecontrols;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import net.sourceforge.cruisecontrol.CruiseControlConfig;
import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.Modification;
import net.sourceforge.cruisecontrol.ProjectQuery;
import net.sourceforge.cruisecontrol.gendoc.annotations.Default;
import net.sourceforge.cruisecontrol.gendoc.annotations.Description;
import net.sourceforge.cruisecontrol.gendoc.annotations.DescriptionFile;
import net.sourceforge.cruisecontrol.gendoc.annotations.Optional;
import net.sourceforge.cruisecontrol.gendoc.annotations.Required;
import net.sourceforge.cruisecontrol.util.ValidationHelper;

import org.apache.log4j.Logger;

/**
 * Modifi TOD: DOPSAT!!!
 * @author dtihelka
 */
@DescriptionFile
public class ProjectStatus extends FakeUserSourceControl {

  private static final long serialVersionUID = 5158569043922879751L;
  private static final Logger LOG = Logger.getLogger(ProjectStatus.class);
  
  /* All 4 copied from BuildStatus#getModifications() */
  public static final String MOST_RECENT_LOGDIR_KEY = "most.recent.logdir";
  public static final String MOST_RECENT_LOGFILE_KEY = "most.recent.logfile";
  public static final String MOST_RECENT_LOGTIME_KEY = "most.recent.logtime";
  public static final String MOST_RECENT_LOGLABEL_KEY = "most.recent.loglabel";
  
  /** Data get by {@link #getProperties()} */
  private final SourceControlProperties properties = new SourceControlProperties(); 
  
  /** Value set through {@link #setVetoIfModified(boolean)} */
  private boolean vetoIfMdified = false;
  /** Value set through {@link #setTriggerOnSuccess(boolean)} */
  private boolean triggerOnSuccess = true;
  /** Value set through {@link #setProject(String)} */
  private String projectName;
  /** Interface to the project to be monitored */
  private ProjectQuery project;
  
  
  @Override
  public Map<String, String> getProperties() {
    return properties.getPropertiesAndReset();
  }
  
  @Override
  @Description("Will set this property to 'true' if a modification has occurred. For use in conditionally "
          + "controlling the build later.")
  @Optional
  public void setProperty(String propertyName) {
      properties.assignPropertyName(propertyName);
  }
  
  @SuppressWarnings("javadoc")
  @Description("The name of project to be monitored.")
  @Required
  public void setProject(String name) {
      projectName = name;
  }
  
  @SuppressWarnings("javadoc")
  @Description("When set to <i>true</i>, the veto of build is signalized when a modificationis found "
          + "in the monitored project")
  @Optional
  @Default("false")
  public void setVetoIfModified(boolean val) {
      vetoIfMdified = val;
  }
  
  @SuppressWarnings("javadoc")
  @Description("When set to <i>false</i>, the module will never trigger any build, no matterthe monitored "
          + "project is triggered. However, the veto of the build can still besignalized, if configured so.")
  @Optional
  @Default("true")
  public void setTriggerOnSuccess(boolean val) {
    triggerOnSuccess = val;
  }
  
  @Override
  public void validate() throws CruiseControlException  {
      /* Project name is required */
      ValidationHelper.assertIsSet(projectName, "project", getClass());
    
      /* Get the project and check if exists */
      project = CruiseControlConfig.findProject(projectName);
      ValidationHelper.assertTrue(projectName.equals(project.getName()), "Mismatch in project names, want "
              + projectName + ", get " + project.getName());
  }
  
  @Override
  public List<Modification> getModifications(Date lastBuild, Date now) {
      final List<Modification> modifications = new ArrayList<Modification>();
      final List<Modification> projModifs = project.modificationsSinceLastBuild();
      final Date lastSuccess = project.successLastBuild();
    
      /* There are modification in the monitored project since its last build. So, veto the build,
       * if any of the modifications is more recent than the lastBuild value of this project */
      if (vetoIfMdified && !projModifs.isEmpty()) {
          for (Modification mod : projModifs) {
              if (mod.getModifiedTime().after(lastBuild)) {
                  throw new ProjectStatus.VetoException("Modifications in " + project.getName()
                          + " found since its build on " + lastSuccess);
              }
          }
      }
      /* If the last successful build of the monitored project is more recent than the last build of the
       * current project, and the build is required to be triggered, fill the list of modifications in
       * the monitored project occurred since the last build of this project. */
      if (lastSuccess.after(lastBuild) && triggerOnSuccess) {
          final Modification summary = new Modification("buildstatus");
          final Date modifiedTime = project.successLastBuild();
          final String revision = project.successLastLabel();
      
          modifications.add(summary);
          modifications.addAll(project.modificationsSince(lastBuild));
          modifications.addAll(projModifs);
      
          Date lastModif = new Date();
          for (Modification mod : projModifs) {
              if (lastModif.before(mod.getModifiedTime())) {
                  lastModif = mod.getModifiedTime();
              }
          }
          /* Fill properties, Code copied from BuildStatus#getModifications() */
          properties.put("most.recent.logdir", project.getLogDir());
          properties.put("most.recent.logfile", project.successLastLog());
          properties.put("most.recent.logtime", new SimpleDateFormat("yyyyMMddHHmmss").format(modifiedTime));
          properties.put("most.recent.loglabel", revision);
      }
      
      return finalizeModifications(modifications);
  }
  
  /**
   * Calls {@link SourceControlProperties#modificationFound()} is a modification has been found.
   * @param modifications the list of modifications found
   * @return modifications array back (unchanged)
   */
  private List<Modification> finalizeModifications(List<Modification> modifications) {
      if (!modifications.isEmpty()) {
          properties.modificationFound();
      }
      return modifications;
  }
  
  /** Exception thrown o signalize the cancel of the build */
  private class VetoException extends RuntimeException {
    
      @SuppressWarnings("javadoc")
      public VetoException(String message) {
          super(message);
      }

      /** Setialzation UID */
      private static final long serialVersionUID = 1L;
  }
}
 

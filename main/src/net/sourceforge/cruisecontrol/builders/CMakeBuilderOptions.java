package net.sourceforge.cruisecontrol.builders;

import java.util.LinkedList;

import net.sourceforge.cruisecontrol.Builder.EnvConf;
import net.sourceforge.cruisecontrol.builders.CMakeBuilder.Option;

/**
 * Class holding a set of pre-configured options for <code>cmake</code> builder. The set of 
 * options can be pre-configured as
 * as:
 * <pre>
 *    <plugin name="XXX" class=zcu.kky.Options>
 *      <option value="..." />
 *      <option value="..." />
 *      ...
 *    </plugin>
 *    <plugin name="YYY" class=zcu.kky.Options>
 *      ...
 *    </plugin>
 *    <plugin name="ZZZ" class=zcu.kky.Options>
 *      ...
 *    </plugin>
 * </pre>
 * 
 * and used to configure CMake as:
 * 
 * <pre>
 *    <cmake ...>
 *      <XXX/>
 *      <YYY/>
 *      <option value="..." />
 *    </cmake>
 *    <cmake ...>
 *      <XXX/>
 *      <ZZZ/>
 *    </cmake>
 * </pre>
 */
public final class CMakeBuilderOptions   {
      
    /**
     * Creates object into which <code><option /></code> tag will be set. Each call returns new object which is
     * expected to be set by CC. The attribute is not required.
     *
     * @return new object to configure according to the tag values.
     * @see    CMakeBuilder#createOption()
     */
    public Object  createOption() {
        options.add(new Option());
        return options.getLast();
    }
    
    /**
     * @return new {@link EnvConf} object to configure.
     */
    public EnvConf createEnv() {
        envs.add(new EnvConf());
        return envs.getLast();
    } // createEnv
    
    
    /**
     * Gets the options set through {@link #createOption()}.
     * @return iterator through the sequence of options
     */
    Iterable<Option> getOptions() {
        return options;
    }

    /**
     * Gets the env variable set through {@link #createEnv()}.
     * @return iterator through the sequence of env values
     */
    Iterable<EnvConf> getEnvs() {
        return envs;
    }
      
    /** The list of <tt>-D</tt> defines passed to <tt>cmake</tt> command. */
    private final LinkedList<Option> options = new LinkedList<Option>();
    private final LinkedList<EnvConf> envs = new LinkedList<EnvConf>();
  }
  

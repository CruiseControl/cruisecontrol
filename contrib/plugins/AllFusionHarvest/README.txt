As a "contrib" plugin, this plugin is not bundled with CruiseControl automatically.
You may, however, build this plugin yourself and register it with your CruiseControl
server. To do this, you must put the compiled plugin on your CruiseControl server's
classpath. Then, either add the plugin's XML node name and class name to the
main/src/net/sourceforge/cruisecontrol/default-plugins.properties file or register
it in your server's config.xml file using a <plugin> tag. See
http://cruisecontrol.sourceforge.net/main/plugins.html#registration for more
information.

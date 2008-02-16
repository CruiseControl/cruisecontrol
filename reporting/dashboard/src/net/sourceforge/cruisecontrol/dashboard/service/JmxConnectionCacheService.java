package net.sourceforge.cruisecontrol.dashboard.service;

import net.sourceforge.cruisecontrol.BuildLoopInformation.JmxInfo;
import net.sourceforge.cruisecontrol.dashboard.repository.BuildInformationRepository;
import org.aspectj.lang.ProceedingJoinPoint;

import javax.management.MBeanServerConnection;
import java.util.HashMap;
import java.util.Map;

public class JmxConnectionCacheService {

    private BuildInformationRepository buildInfoRepository;

    private Map cachedConnections = new HashMap();

    public JmxConnectionCacheService(BuildInformationRepository buildInfoRepository) {
        this.buildInfoRepository = buildInfoRepository;
    }

    public MBeanServerConnection getJMXConnection(ProceedingJoinPoint pjp, String projectName)
            throws Throwable {
        JmxInfo newJmxInfo = buildInfoRepository.getBuildLoopInfo(projectName).getJmxInfo();
        if (!cachedConnections.containsKey(newJmxInfo)) {
            cachedConnections.put(newJmxInfo, pjp.proceed());
        }
        return (MBeanServerConnection) cachedConnections.get(newJmxInfo);
    }
}

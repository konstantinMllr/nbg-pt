package nbgpt.service;

import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

@Service
public class IpBlockingService {
    private static final int MAX_ENTRIES = 100;

    private final Set<String> blockedIps;

    public IpBlockingService() {
        this.blockedIps = Collections.newSetFromMap(new LinkedHashMap<String, Boolean>() {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, Boolean> eldest) {
                return size() > MAX_ENTRIES;
            }
        });
    }

    public synchronized void blockIp(String ip) {
        blockedIps.add(ip);
    }

    public synchronized boolean isBlocked(String ip) {
        return blockedIps.contains(ip);
    }
}


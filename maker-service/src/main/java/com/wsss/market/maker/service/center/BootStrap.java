package com.wsss.market.maker.service.center;

import com.cmcm.finance.ccc.client.CoinConfigCenterClient;
import com.wsss.market.maker.model.config.BiAnConfig;
import com.wsss.market.maker.model.config.MakerConfig;
import com.wsss.market.maker.model.config.SymbolConfig;
import com.wsss.market.maker.service.thread.pool.MarkerMakerThreadPool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Component
public class BootStrap implements ApplicationListener<ContextRefreshedEvent>, InitializingBean, DisposableBean {
    private static boolean start = false;
    @Resource
    private BiAnConfig biAnConfig;
    @Resource
    private DataCenter dataCenter;
    @Resource
    private SymbolConfig symbolConfig;
    @Resource
    private MakerConfig makerConfig;
    @Resource
    private CoinConfigCenterClient coinConfigCenterClient;

    @Override
    public void destroy() throws Exception {

    }

    @Override
    public void afterPropertiesSet() throws Exception {
        ConfigCenter.setApolloConfig(biAnConfig);
    }

    @Override
    public void onApplicationEvent(ContextRefreshedEvent contextRefreshedEvent) {
        if (start) {
            return;
        }
        log.info("启动应用");
        start = true;

        reload();
        MarkerMakerThreadPool.getShareExecutor().scheduleWithFixedDelay(() -> {
            reload();
        }, symbolConfig.getReloadTime(), symbolConfig.getReloadTime(), TimeUnit.SECONDS);
        MarkerMakerThreadPool.getShareExecutor().scheduleWithFixedDelay(() -> {
            dataCenter.wakeUpDepthAllSymbol();
        }, makerConfig.getSyncTime(), makerConfig.getSyncTime(), TimeUnit.SECONDS);
    }

    public void reload() {
        try {
            Set<String> set = coinConfigCenterClient.getAllSymbolInfo().stream()
                    .filter(s -> symbolConfig.getSupportSymbols().contains(s.getSymbolName()) || symbolConfig.getSupportSymbols().contains("all"))
                    .filter(s -> {
                        int hash = Math.abs(s.hashCode()) % 10000;
                        return hash >= symbolConfig.getStartHash() && hash <= symbolConfig.getEndHash();
                    })
                    .map(s -> s.getSymbolName())
                    .collect(Collectors.toSet());

            Set<String> add = set.stream().filter(s -> !dataCenter.isRegistered(s)).collect(Collectors.toSet());
            Set<String> remove = dataCenter.registeredSymbols().stream().filter(s -> !set.contains(s)).collect(Collectors.toSet());

            if(!add.isEmpty()) {
                dataCenter.register(add);
            }
            if(!remove.isEmpty()) {
                dataCenter.remove(remove);
            }
        } catch (Exception e) {
            log.error("reload error:{}", e);
        }

    }


}

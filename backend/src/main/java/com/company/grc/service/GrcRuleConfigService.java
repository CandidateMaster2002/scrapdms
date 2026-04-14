package com.company.grc.service;

import com.company.grc.entity.GrcRuleConfigEntity;
import com.company.grc.repository.GrcRuleConfigRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.PostConstruct;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Manages configurable GRC rule weightages stored in the database.
 * Provides get/set for all rule multipliers and max scores.
 * Seeds defaults on startup if the table is empty.
 */
@Service
public class GrcRuleConfigService {

    private final GrcRuleConfigRepository repo;

    @Autowired
    public GrcRuleConfigService(GrcRuleConfigRepository repo) {
        this.repo = repo;
    }

    // ── Default values (mirrors GrcScoreConfig.java) ──────────────────────

    public static final Map<String, Object[]> DEFAULTS = new LinkedHashMap<>();
    static {
        // [configValue, description]
        DEFAULTS.put("TYPE_MAX",          new Object[]{10.0, "Type Rule — Max score"});
        DEFAULTS.put("TYPE_PUBLIC_MULT",  new Object[]{0.0,  "Type Rule — Public Limited multiplier"});
        DEFAULTS.put("TYPE_PRIVATE_MULT", new Object[]{0.25, "Type Rule — Private Limited multiplier"});
        DEFAULTS.put("TYPE_PARTNER_MULT", new Object[]{0.5,  "Type Rule — Partnership multiplier"});
        DEFAULTS.put("TYPE_PROPR_MULT",   new Object[]{1.0,  "Type Rule — Proprietorship multiplier"});
        DEFAULTS.put("TYPE_OTHER_MULT",   new Object[]{1.0,  "Type Rule — Other type multiplier"});
        DEFAULTS.put("TYPE_COMPANY_MULT", new Object[]{0.0,  "Type Rule — General company multiplier (legacy)"});

        DEFAULTS.put("REG_MAX",           new Object[]{10.0, "Registration Date — Max score"});
        DEFAULTS.put("REG_LT1_MULT",      new Object[]{1.0,  "Registration Date — < 1 year multiplier"});
        DEFAULTS.put("REG_1TO3_MULT",     new Object[]{0.75, "Registration Date — 1–3 years multiplier"});
        DEFAULTS.put("REG_3TO5_MULT",     new Object[]{0.5,  "Registration Date — 3–5 years multiplier"});
        DEFAULTS.put("REG_GT5_MULT",      new Object[]{0.0,  "Registration Date — > 5 years multiplier"});

        DEFAULTS.put("TRN_MAX",           new Object[]{5.0,  "Turnover — Max score"});
        DEFAULTS.put("TRN_LT50_MULT",     new Object[]{1.0,  "Turnover — < 50 Cr multiplier"});
        DEFAULTS.put("TRN_50TO100_MULT",  new Object[]{0.5,  "Turnover — 50–100 Cr multiplier"});
        DEFAULTS.put("TRN_GT100_MULT",    new Object[]{0.0,  "Turnover — > 100 Cr multiplier"});

        DEFAULTS.put("STS_MAX",           new Object[]{35.0, "GSTN Status — Max score"});
        DEFAULTS.put("STS_ACTIVE_MULT",   new Object[]{0.0,  "GSTN Status — Active multiplier"});
        DEFAULTS.put("STS_CANCEL_MULT",   new Object[]{1.0,  "GSTN Status — Cancelled multiplier"});

        DEFAULTS.put("G1_MAX",            new Object[]{20.0, "GSTR-1 Filing — Max score"});
        DEFAULTS.put("G1_THRESHOLD",      new Object[]{1.0,  "GSTR-1 Filing — Delay threshold (count)"});
        DEFAULTS.put("G1_OK_MULT",        new Object[]{0.0,  "GSTR-1 Filing — On-time multiplier"});
        DEFAULTS.put("G1_DELAY_MULT",     new Object[]{1.0,  "GSTR-1 Filing — Delayed multiplier"});

        DEFAULTS.put("G3B_MAX",           new Object[]{20.0, "GSTR-3B Filing — Max score"});
        DEFAULTS.put("G3B_THRESHOLD",     new Object[]{1.0,  "GSTR-3B Filing — Delay threshold (count)"});
        DEFAULTS.put("G3B_OK_MULT",       new Object[]{0.0,  "GSTR-3B Filing — On-time multiplier"});
        DEFAULTS.put("G3B_DELAY_MULT",    new Object[]{1.0,  "GSTR-3B Filing — Delayed multiplier"});

        DEFAULTS.put("COLOR_RED_THRESHOLD",    new Object[]{30.0, "Color Coding — Red Score Threshold"});
        DEFAULTS.put("COLOR_YELLOW_THRESHOLD", new Object[]{20.0, "Color Coding — Yellow Score Threshold"});
    }

    private Map<String, Double> cachedConfigMap = null;

    /** Seed defaults into DB if the table is empty. */
    @PostConstruct
    @Transactional
    public void seedDefaults() {
        DEFAULTS.forEach((key, meta) -> {
            if (!repo.existsById(key)) {
                repo.save(GrcRuleConfigEntity.builder()
                        .configKey(key)
                        .configValue((Double) meta[0])
                        .description((String) meta[1])
                        .build());
            }
        });
        refreshCache();
    }

    /** Returns all config entries from DB as a List. */
    @Transactional(readOnly = true)
    public List<GrcRuleConfigEntity> getAllConfig() {
        Map<String, Double> map = getConfigMap();
        return map.entrySet().stream().map(entry -> {
            String key = entry.getKey();
            Double val = entry.getValue();
            String desc = (String) DEFAULTS.getOrDefault(key, new Object[]{0.0, key})[1];
            return GrcRuleConfigEntity.builder()
                    .configKey(key)
                    .configValue(val)
                    .description(desc)
                    .build();
        }).toList();
    }

    /** Returns all config values as a key→value map (cached). */
    public Map<String, Double> getConfigMap() {
        if (cachedConfigMap == null) {
            refreshCache();
        }
        return cachedConfigMap;
    }

    /** Refreshes the local memory cache from the database. */
    private synchronized void refreshCache() {
        Map<String, Double> map = new LinkedHashMap<>();
        // Start with defaults
        DEFAULTS.forEach((k, v) -> map.put(k, (Double) v[0]));
        // Override with DB values
        repo.findAll().forEach(e -> map.put(e.getConfigKey(), e.getConfigValue()));
        cachedConfigMap = map;
    }

    /** Get a single config value by key (from cache). */
    public double get(String key) {
        return getConfigMap().getOrDefault(key, 0.0);
    }

    /** Save / update a batch of config values. */
    @Transactional
    public List<GrcRuleConfigEntity> saveConfig(Map<String, Double> updates) {
        updates.forEach((key, value) -> {
            GrcRuleConfigEntity entity = repo.findById(key).orElseGet(() ->
                    GrcRuleConfigEntity.builder()
                            .configKey(key)
                            .description((String) DEFAULTS.getOrDefault(key, new Object[]{0.0, key})[1])
                            .build());
            entity.setConfigValue(value);
            repo.save(entity);
        });
        refreshCache();
        return repo.findAll();
    }
}

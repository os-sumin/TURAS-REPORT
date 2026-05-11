package com.enftrms.prediction.service;

import com.enftrms.prediction.domain.ScoreCardRuleSet;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@Component
public class ScoreCardLoader {

    @Value("classpath:rules/scorecard.yml")
    private Resource ruleResource;

    private ScoreCardRuleSet ruleSet;
    private String rawYaml;

    @PostConstruct
    public void load() throws IOException {
        try (InputStream is = ruleResource.getInputStream()) {
            this.rawYaml = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            Yaml yaml = new Yaml(new Constructor(ScoreCardRuleSet.class));
            this.ruleSet = yaml.loadAs(this.rawYaml, ScoreCardRuleSet.class);
        }
    }

    public ScoreCardRuleSet getRuleSet()  { return ruleSet; }
    public String getRawYaml()            { return rawYaml; }
    public String getRuleVersion()        { return ruleSet != null ? ruleSet.getRuleVersion() : null; }
}

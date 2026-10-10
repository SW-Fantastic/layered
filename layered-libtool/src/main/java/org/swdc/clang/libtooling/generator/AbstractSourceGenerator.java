package org.swdc.clang.libtooling.generator;

import freemarker.template.Configuration;
import freemarker.template.Template;

import java.util.List;

public abstract class AbstractSourceGenerator {

    private Configuration cfg;

    protected synchronized Configuration getConfigure() {

        if (cfg != null) {
            return cfg;
        }
        cfg = new Configuration(Configuration.VERSION_2_3_23);
        cfg.setInterpolationSyntax(Configuration.SQUARE_BRACKET_INTERPOLATION_SYNTAX);
        cfg.setClassForTemplateLoading(getClass(),"");
        cfg.setDefaultEncoding("UTF-8");
        cfg.setNumberFormat("computer");
        return cfg;

    }

    /**
     * 根据模板名称获取对应的 FreeMarker 模板对象
     * <p>
     * 该方法会先校验模板名称是否存在于当前生成器所声明的模板列表中，
     * 若存在则以该名称拼接 ".ftl" 后缀从配置中加载模板。
     *
     * @param name 模板名称（不含 ".ftl" 扩展名）
     * @return 对应的 FreeMarker 模板对象
     * @throws RuntimeException 当模板不存在或模板加载失败时抛出
     */
    protected Template getTemplate(String name) {

        try {

            if (!getTemplateNames().contains(name + ".ftl")) {
                throw new IllegalArgumentException("template not found");
            }
            return getConfigure().getTemplate(name + ".ftl");

        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    protected abstract List<String> getTemplateNames();



}

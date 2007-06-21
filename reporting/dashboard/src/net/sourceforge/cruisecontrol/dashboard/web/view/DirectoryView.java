package net.sourceforge.cruisecontrol.dashboard.web.view;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.sourceforge.cruisecontrol.dashboard.service.TemplateRenderService;
import net.sourceforge.cruisecontrol.dashboard.utils.DashboardUtils;

import org.apache.commons.lang.StringUtils;

public class DirectoryView extends BaseFileView {

    private final TemplateRenderService service;

    public DirectoryView(TemplateRenderService service) {
        this.service = service;
    }

    private void handleDir(File file, HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        File[] files = file.listFiles();
        PrintWriter writer = response.getWriter();
        writer.write("<ul style=\"margin-left:1em\">");
        Map params = null;
        for (int i = 0; i < files.length; i++) {
            if (files[i].isHidden()) {
                continue;
            }
            params = new HashMap();
            String url = this.getUrl(request, files[i].getName());
            params.put("$url", url);
            params.put("$fileName", files[i].getName());
            params.put("$id", StringUtils.replaceChars(url, '/', '_'));
            String html =
                    service.renderTemplate(DashboardUtils.getFileType(files[i]) + ".template",
                            params);
            writer.write(html);
        }
        writer.write("</ul>");
    }

    public String getContentType() {
        return "text/html";
    }

    public void render(Map model, HttpServletRequest request, HttpServletResponse response)
            throws Exception {
        File file = (File) model.get("targetFile");
        handleDir(file, request, response);
    }
}

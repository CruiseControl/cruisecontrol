package net.sourceforge.cruisecontrol.dashboard.web.validator;

import java.io.File;

import net.sourceforge.cruisecontrol.dashboard.web.command.DownLoadFile;

import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

public class DownLoadFileValidator implements Validator {

    private static final String DOWNLOAD_FILE = "logFile";

    public boolean supports(Class clazz) {
        return true;
    }

    public void validate(Object cmd, Errors errors) {
        File expectedFile = ((DownLoadFile) cmd).getDownLoadFile();
        if (expectedFile == null) {
            errors.reject(DOWNLOAD_FILE, "File can not be retrieved.");
            return;
        }
        if (!expectedFile.exists()) {
            errors.reject(DOWNLOAD_FILE, "File does not exist.");
            return;
        }
    }
}

package co.rob.io;

import co.rob.pojo.ImagePathPreference;
import co.rob.util.ImageFileReadException;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.File;
import java.io.IOException;
import java.util.Optional;

@Singleton
public class ImageFileReader {

    @Inject
    public ImageFileReader() {
        // default constructor for Dagger
    }

    // validate the image file
    // NOTE: a directory is valid because it indicates the absolute
    // path to recursive files
    public File validateImageFile(ImagePathPreference imagePathPreference, File imageFilePath, String customImageFileTextFieldText) throws ImageFileReadException {
        Optional<File> identifiedImageFile = identifyImageFile(imagePathPreference, imageFilePath, customImageFileTextFieldText);
        if (imagePathPreference == ImagePathPreference.NONE) {
            return null;
        }
        if (identifiedImageFile.isEmpty()) {
            return null;
        }
        File imageFile = identifiedImageFile.get();

        if (!imageFile.canRead() && !imageFile.isAbsolute()) {
            throw new ImageFileReadException("Image file \"" + imageFile.getPath() + "\"\ncannot be read, likely because it is defined with a relative path." + "\nPlease click on \"Select custom path\" to select or type in an absolute path.");
        } else if (!imageFile.canRead()) {
            throw new ImageFileReadException("Image file \"" + imageFile.getPath() + "\"\ncannot be read.");
        } else {
            // good, the file is valid, normalize it
            try {
                return imageFile.getCanonicalFile();
            } catch (IOException ioe) {
                throw new ImageFileReadException("Image file \"" + imageFile.getPath() + "\"\ncannot be read: " + ioe);
            }
        }
    }

    private Optional<File> identifyImageFile(ImagePathPreference imagePathPreference, File imageFilePath, String customImageFileTextFieldText) throws ImageFileReadException {
        if (imagePathPreference == ImagePathPreference.DEFAULT) {
            if (imageFilePath == null) {
                throw new ImageFileReadException("The Image File path could not be obtained from the report." + "\nThe Image file may have been moved or the Report File may be invalid." + "\nPlease click on \"Select custom path\" to select an Image File path for this Report file.");
            }
        } else if (imagePathPreference == ImagePathPreference.CUSTOM) {
            return Optional.of(new File(customImageFileTextFieldText));
        } else if (imagePathPreference == ImagePathPreference.NONE) {
            return Optional.empty();
        } else {
            throw new RuntimeException("Invalid type");
        }
        return Optional.empty();
    }
}

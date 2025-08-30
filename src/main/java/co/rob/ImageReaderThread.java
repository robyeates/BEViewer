package co.rob;

import java.io.File;

public class ImageReaderThread extends Thread {
  // input values
  private File imageFile;
  private String forensicPath;
  private int numBytes;

  // output values
  public ImageReader.ImageReaderResponse response;
  public boolean isDone = false;
 
  // resources
  private final ImageModel imageModel;
  private final ImageReaderManager imageReaderManager;

  /**
   * The <code>ImageReaderThread</code> class performs reading from an image
   * into <code>ImageReaderResponse</code>.
   * imageModel.manageModelChanges is called when done, which consumes the response.
   */
  public ImageReaderThread(ImageModel imageModel,
                           ImageReaderManager imageReaderManager,
                           File imageFile,
                           String forensicPath,
                           int numBytes) {

    this.imageModel = imageModel;
    this.imageReaderManager = imageReaderManager;
    this.imageFile = imageFile;
    this.forensicPath = forensicPath;
    this.numBytes = numBytes;
  }

  // run the image reader
  public void run() {

    // handle the read request
    try {
      // issue the read
      response = imageReaderManager.read(imageFile, forensicPath, numBytes);

      // note if no bytes were returned
      if (response.bytes.length == 0) {
        WError.showMessageLater("No bytes were read from the image path, likely because the image file is not aviailable.", "No Data");
      }

    } catch (Exception e) {
      // on any failure: warn and clear values
      WError.showErrorLater("Unable to read the Image.\n"
                            + "file: " + imageFile + " forensic path: " + forensicPath,
                            "Error reading Image", e);

      response = new ImageReader.ImageReaderResponse(new byte[0], 0);
    }
    isDone = true;

    // update the image model
    imageModel.manageModelChanges();
  }
}


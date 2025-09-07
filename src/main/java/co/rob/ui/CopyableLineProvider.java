package co.rob.ui;

/**
 * An standard interface for copying a line.
 */
public interface CopyableLineProvider {

  /**
   * Returns the requested line as a string.
   */
  public String getCopyableLine(int line);
}


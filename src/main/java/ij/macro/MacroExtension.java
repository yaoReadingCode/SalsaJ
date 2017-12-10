package ij.macro;

public interface MacroExtension {
  int ARG_STRING = 0x01;
  int ARG_NUMBER = 0x02;
  int ARG_ARRAY  = 0x04;
  
  int ARG_OUTPUT = 0x10;
  int ARG_OPTIONAL = 0x20;

  String handleExtension(String name, Object[] args);
  
  ExtensionDescriptor[] getExtensionFunctions();
}

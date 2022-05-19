module security {
    requires image;
    requires miglayout;
    requires java.prefs;
    requires java.desktop;
    requires com.google.common;
    requires com.google.gson;
    opens com.udacity.catpoint.data to com.google.gson;
}
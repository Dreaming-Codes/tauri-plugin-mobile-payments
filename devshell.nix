{ pkgs }:

with pkgs;
let
  rustToolchain = pkgs.rust-bin.stable.latest.default.override {
    targets = ["aarch64-linux-android" "armv7-linux-androideabi" "x86_64-linux-android" "i686-linux-android"];
  };
in
# Configure your development environment.
#
# Documentation: https://github.com/numtide/devshell
devshell.mkShell {
  name = "android-project";
  motd = ''
    Entered the Android app development environment.
  '';
  env = [
    {
      name = "ANDROID_HOME";
      value = "${android-sdk}/share/android-sdk";
    }
    {
      name = "ANDROID_SDK_ROOT";
      value = "${android-sdk}/share/android-sdk";
    }
    {
      name = "JAVA_HOME";
      value = jdk17.home;
    }
  ];
  packages = [
    android-sdk
    rustToolchain
    cargo-expand
    gradle
    jdk17
  ];
  devshell.startup.installCargoCliTools.text = ''
    export PATH="$PWD/bin:$PATH"
    if ! type tsync > /dev/null 2> /dev/null; then
      cargo install tsync --root $PWD
    fi
  '';
}

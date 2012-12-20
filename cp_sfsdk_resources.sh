#!/bin/sh

function usage()
{
    echo "================================================================================"
    echo "This script prepares your Android project for SF Android SDK integration."
    echo "It requires access to the github repo root for the salesforce-android-mobile SDK"
    echo
    echo "You can clone the repo to some local directory using the command below "
    echo
    echo " git clone git@github.com:forcedotcom/SalesforceMobileSDK-Android"
    echo "================================================================================"
    echo 
    echo "Usage: "
    echo "  cp_sfsdk_resources.sh <YOUR_PROJECT_DIR> <SF_MOBILE_SDK_GITHUB_REPO_ROOTDIR>"
    echo "  Salesforce Android Mobile SDK Github root dir is required"
    echo 
    echo "  @author: Appirio-appdev"
    echo "  @date:   12/14/2011"
  
    exit 0
}

if [ $# != 2 ]
then
  usage
fi

PROJEC_ROOT=$1
GITHUB_ROOT=$2
if [ ! -d $GITHUB_ROOT ]
then
    echo "Invalid or non-existent directory: " $GITHUB_ROOT
    exit 1
fi
if [ ! -d $PROJEC_ROOT ]
then
    echo "Invalid or non-existent directory: " $PROJEC_ROOT
    exit 1
fi

# create relevant directories
/bin/mkdir -p $PROJEC_ROOT/res/layout
/bin/mkdir -p $PROJEC_ROOT/res/menu
/bin/mkdir -p $PROJEC_ROOT/res/drawable
/bin/mkdir -p $PROJEC_ROOT/res/values

# copy resources

# layout
/bin/cp -f $GITHUB_ROOT/native/TemplateApp/res/layout/custom_server_url.xml $PROJEC_ROOT/res/layout
/bin/cp -f $GITHUB_ROOT/native/TemplateApp/res/layout/header.xml $PROJEC_ROOT/res/layout
/bin/cp -f $GITHUB_ROOT/native/TemplateApp/res/layout/login.xml $PROJEC_ROOT/res/layout
/bin/cp -f $GITHUB_ROOT/native/TemplateApp/res/layout/login_header.xml $PROJEC_ROOT/res/layout
/bin/cp -f $GITHUB_ROOT/native/TemplateApp/res/layout/passcode.xml $PROJEC_ROOT/res/layout
/bin/cp -f $GITHUB_ROOT/native/TemplateApp/res/layout/server_picker.xml $PROJEC_ROOT/res/layout

# menu
/bin/cp -f $GITHUB_ROOT/native/TemplateApp/res/menu/clear_custom_url.xml $PROJEC_ROOT/res/menu


# values
/bin/cp -f $GITHUB_ROOT/native/TemplateApp/res/values/sdk.xml $PROJEC_ROOT/res/values
/bin/cp -f $GITHUB_ROOT/native/TemplateApp/res/values/rest.xml $PROJEC_ROOT/res/values

# drawable (edit icon)
/bin/cp -f $GITHUB_ROOT/native/TemplateApp/res/drawable-hdpi/edit_icon.png $PROJEC_ROOT/res/drawable

echo "Copied all resources."

# now copy templates that need to be altered for your app

# create relevant directories
TEMPLATE_DIR=$PROJEC_ROOT/templates
/bin/mkdir -p $TEMPLATE_DIR
TEMPLATE_SRCDIR=$TEMPLATE_DIR/src/com/salesforce/samples/templateapp
/bin/mkdir -p $TEMPLATE_SRCDIR
/bin/mkdir -p $TEMPLATE_DIR/res/layout
/bin/mkdir -p $TEMPLATE_DIR/res/values
/bin/mkdir -p $TEMPLATE_DIR/res/xml

# manifest
/bin/cp -f $GITHUB_ROOT/native/TemplateApp/AndroidManifest.xml $TEMPLATE_DIR

# source files
/bin/cp -f $GITHUB_ROOT/native/TemplateApp/src/com/salesforce/samples/templateapp/SalesforceRImpl.java $TEMPLATE_SRCDIR
/bin/cp -f $GITHUB_ROOT/native/TemplateApp/src/com/salesforce/samples/templateapp/TemplateApp.java     $TEMPLATE_SRCDIR
/bin/cp -f $GITHUB_ROOT/native/TemplateApp/src/com/salesforce/samples/templateapp/MainActivity.java    $TEMPLATE_SRCDIR

# resources
/bin/cp -f $GITHUB_ROOT/native/TemplateApp/res/layout/main.xml       $TEMPLATE_DIR/res/layout
/bin/cp -f $GITHUB_ROOT/native/TemplateApp/res/values/strings.xml    $TEMPLATE_DIR/res/values
/bin/cp -f $GITHUB_ROOT/native/TemplateApp/res/xml/authenticator.xml $TEMPLATE_DIR/res/xml

echo "Copied all relevant templates to "$TEMPLATE_DIR". Please use files in there as baselines for your Android app."
echo "Done."




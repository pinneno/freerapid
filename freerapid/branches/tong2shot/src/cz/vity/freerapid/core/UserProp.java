package cz.vity.freerapid.core;

/**
 * @author Vity
 */
final public class UserProp {


    private UserProp() {
    }

    public static final String PLUGINSSTATUS_URL = "pluginsStatusUrl";

    public static final String LAST_IMPORT_FILTER = "lastUsedImportFilter";
    public static final String IMPORT_LAST_USED_FOLDER = "importLastUsedFolder";

    public static final String LAST_EXPORT_FILENAME = "lastExportFilename";
    public static final String LAST_EXPORT_FILTER = "lastExportFilter";
    public static final String LAST_USED_FOLDER_EXPORT = "lastUsedFolderExport";

    public static final String USE_TEMPORARY_FILES = "useTemporaryFiles";
    public static final boolean USE_TEMPORARY_FILES_DEFAULT = true;

    public static final String DOWNLOAD_ON_APPLICATION_START = "downloadOnStart";
    public static final boolean DOWNLOAD_ON_APPLICATION_START_DEFAULT = true;

    public static final String FILE_ALREADY_EXISTS = "whatToDoIfFileAlreadyExists";

    public static final int RENAME = 0;
    public static final int OVERWRITE = 1;
    public static final int SKIP = 2;
    public static final int ASK = 3;


    public static final String PLAY_SOUNDS_OK = "playSoundOK";//OK
    public static final String LAST_USED_SAVED_PATH = "lastUsedSavedPaths";

    public static final String LAST_COMBO_PATH = "lastComboPath";


    public static final String CUSTOM_TOOLBAR_BUTTONS = "customToolbarButtons";
    public static final String CUSTOM_TOOLBAR_BUTTONS_DEFAULT = "A-BCD-EFGH";

    public static final String MAX_DOWNLOADS_AT_A_TIME = "maxDownloadsAtATime";
    public static final int MAX_DOWNLOADS_AT_A_TIME_DEFAULT = 5;

    public static final String AUTO_RECONNECT_TIME = "autoReconnectTime";
    public static final int AUTO_RECONNECT_TIME_DEFAULT = 120;

    public static final String ERROR_ATTEMPTS_COUNT = "errorAttemptsCount";
    public static final int ERROR_ATTEMPTS_COUNT_DEFAULT = 5;

    public static final String DOWNLOADED_HISTORY_FILE_NAME = "downloads.txt";

    public static final String CONTAIN_DOWNLOADS_FILTER = "containDownloadsFilter";

    public static final String CHECK_RECENT_DOWNLOAD_HISTORY = "checkRecentDownloadHistory";
    public static final boolean CHECK_RECENT_DOWNLOAD_HISTORY_DEFAULT = true;

    public static final String SELECTED_DOWNLOADS_FILTER = "selectedDownloadsFilter";

    public static final String SHOW_COMPLETED = "removeCompleted";

    public static final String USE_DEFAULT_CONNECTION = "useDefaultConnection";
    public static final boolean USE_DEFAULT_CONNECTION_DEFAULT = true;

    public static final String USE_SYSTEM_ICONS = "useSystemIcons";

    public static final String CLOSE_APPLICATION_CONFIRM_WAITTIME = "closeApplicationConfirmTime";

    public static final String USE_PROXY_LIST = "useProxyList";
    public static final boolean USE_PROXY_LIST_DEFAULT = false;

    public static final String PROXY_LIST_PATH = "proxyListPath";
    public static final String PLAY_SOUNDS_FAILED = "playSoundsFailed";
    public static final int FILE_ALREADY_EXISTS_DEFAULT = UserProp.ASK;

    public static final String ERROR_SLEEP_TIME = "firstSleepTime";
    public static final int ERROR_SLEEP_TIME_DEFAULT = 4;

    public static final String START_FROM_TOP = "startDownloadFromTheTop";
    public static final boolean START_FROM_TOP_DEFAULT = true;

    public static final String SHOWINFO_IN_TITLE = "showInfoInFrameTitle";
    public static final boolean SHOWINFO_IN_TITLE_DEFAULT = false;

    //ukladani fronty pro pripad padu programu atd.
    public static final String AUTOSAVE_ENABLED = "autosaveEnabled";
    public static final boolean AUTOSAVE_ENABLED_DEFAULT = true;

    public static final String AUTOSAVE_TIME = "autosaveTime";
    public static final int AUTOSAVE_TIME_DEFAULT = 10;//seconds

    public static final String CLIPBOARD_MONITORING = "clipboardMonitoring";
    public static final boolean CLIPBOARD_MONITORING_DEFAULT = true;

    public static final String ANIMATE_ICON = "animateIcon";
    public static final boolean ANIMATE_ICON_DEFAULT = true;

    public static final String GENERATE_DESCRIPT_ION_FILE = "generateDescript-ionFile";
    public static final boolean GENERATE_DESCRIPT_ION_FILE_DEFAULT = false;

    public static final String GENERATE_DESCRIPTION_BY_FILENAME = "generateDescriptionByFileName";
    public static final boolean GENERATE_DESCRIPTION_BY_FILENAME_DEFAULT = false;

    public static final String GENERATE_DESCRIPTION_FILES_HIDDEN = "descriptionFilesHidden";
    public static final boolean GENERATE_DESCRIPTION_FILES_HIDDEN_DEFAULT = false;

    public static final String SHOW_GRID_HORIZONTAL = "showHorizontalGridLines";
    public static final boolean SHOW_GRID_HORIZONTAL_DEFAULT = false;

    public static final String SHOW_GRID_VERTICAL = "showVerticalGridLines";
    public static final boolean SHOW_GRID_VERTICAL_DEFAULT = false;

    public static final String ANTI_FRAGMENT_FILES = "preCreateFile";
    public static final boolean ANTI_FRAGMENT_FILES_DEFAULT = false;

    public static final String OUTPUT_FILE_BUFFER_SIZE = "outputFileBufferSize";
    public static final String INPUT_BUFFER_SIZE = "inputBufferSize";

    public static final String PAYPAL = "paypal";
    public static final String PAYPAL_DEFAULT = "http://vity.cz/freerapid/paypal";
    public static final String DEMO_URL = "demoURL";

    public static final String BRING_TO_FRONT_WHEN_PASTED = "bringToFrontWhenPasted";
    public static final boolean BRING_TO_FRONT_WHEN_PASTED_DEFAULT = true;

    public static final String REMOVE_COMPLETED_DOWNLOADS = "removeCompletedDownloads";
    public static final int REMOVE_COMPLETED_DOWNLOADS_NEVER = 0;
    public static final int REMOVE_COMPLETED_DOWNLOADS_DEFAULT = REMOVE_COMPLETED_DOWNLOADS_NEVER;
    public static final int REMOVE_COMPLETED_DOWNLOADS_IMMEDIATELY = 1;
    public static final int REMOVE_COMPLETED_DOWNLOADS_AT_STARTUP = 2;

    public static final String SLIM_LINES_IN_HISTORY = "historySlimLines";
    public static final boolean SLIM_LINES_IN_HISTORY_DEFAULT = false;

    public static final String SHOW_PROGRESS_IN_PROGRESSBAR = "showPercentProgress";
    public static final boolean SHOW_PROGRESS_IN_PROGRESSBAR_DEFAULT = false;

    public static final String AUTOSHUTDOWN = "autoshutdown";
    public static final int AUTOSHUTDOWN_DISABLED = 0;
    public static final int AUTOSHUTDOWN_DEFAULT = AUTOSHUTDOWN_DISABLED;
    public static final int AUTOSHUTDOWN_CLOSE = 1;
    public static final int AUTOSHUTDOWN_STANDBY = 2;
    public static final int AUTOSHUTDOWN_REBOOT = 3;
    public static final int AUTOSHUTDOWN_SHUTDOWN = 4;
    public static final int AUTOSHUTDOWN_HIBERNATE = 5;

    public static final String AUTOSHUTDOWN_WITH_ERRORS = "autoshutdownWithErrors";
    public static final boolean AUTOSHUTDOWN_WITH_ERRORS_DEFAULT = false;

    public static final String AUTOSHUTDOWN_DISABLED_WHEN_EXECUTED = "autoshutdownDisabledWhenExecuted";
    public static final boolean AUTOSHUTDOWN_DISABLED_WHEN_EXECUTED_DEFAULT = true;

    public static final String AUTOSHUTDOWN_FORCE = "autoshutdownForce";
    public static final boolean AUTOSHUTDOWN_FORCE_DEFAULT = true;

    public static final String USE_HISTORY = "useHistory";
    public static final boolean USE_HISTORY_DEFAULT = true;

    public static final String CONFIRM_EXITING = "confirmExit";
    public static final boolean CONFIRM_EXITING_DEFAULT = true;

    public static final String CONFIRM_FILE_DELETE = "confirmFileDelete";
    public static final boolean CONFIRM_FILE_DELETE_DEFAULT = true;

    public static final String CONFIRM_REMOVE = "confirmRemove";
    public static final boolean CONFIRM_REMOVE_DEFAULT = true;

    public static final String CONFIRM_DOWNLOADING_REMOVE = "confirmDownloadingRemove";
    public static final boolean CONFIRM_DOWNLOADING_REMOVE_DEFAULT = false;

    public static final String SHOW_MEMORY_INDICATOR = "showMemoryIndicator";
    public static final boolean SHOW_MEMORY_INDICATOR_DEFAULT = false;

    public static final String MAKE_FILE_BACKUPS = "makeFileBackups";
    public static final boolean MAKE_FILE_BACKUPS_DEFAULT = true;

    public static final String DISABLE_CONNECTION_ON_EXCEPTION = "disableConnectionOnException";
    public static final boolean DISABLE_CONNECTION_ON_EXCEPTION_DEFAULT = false;


    public static final String CHECK4_PLUGIN_UPDATES_AUTOMATICALLY = "check4PluginUpdatesAutomatically";
    public static final boolean CHECK4_PLUGIN_UPDATES_AUTOMATICALLY_DEFAULT = true;

    public static final String DOWNLOAD_NOT_EXISTING_PLUGINS = "downloadNotExistingPlugins";
    public static final boolean DOWNLOAD_NOT_EXISTING_PLUGINS_DEFAULT = true;

    public static final String PLUGIN_CHECK_URL_LIST = "pluginCheckUrlList";
    public static final String PLUGIN_CHECK_URL_SELECTED = "pluginCheckUrlSelected";

    public static final String TEST_FILE = "testFiles"; //spusti test pred stahovanim
    public static final boolean TEST_FILE_DEFAULT = true;

    public static final String SHOW_SERVICES_ICONS = "showServicesAsIcons";
    public static final boolean SHOW_SERVICES_ICONS_DEFAULT = true;

    public static final String PLUGIN_UPDATE_CHECK_INTERVAL = "pluginUpdateCheckInterval";
    public static final int PLUGIN_UPDATE_CHECK_INTERVAL_DEFAULT = 24;//hours

    public static final String PLUGIN_LAST_UPDATE_TIMESTAMP_CHECK = "pluginLastUpdateDateTime";

    public static final String DRAG_ON_RIGHT_MOUSE = "dragOnRightMouse";
    public static final boolean DRAG_ON_RIGHT_MOUSE_DEFAULT = true;

    public static final String PLUGIN_UPDATE_METHOD = "pluginUpdateMethod";
    public static final int PLUGIN_UPDATE_ASK_FOR_METHOD = 0;
    public static final int PLUGIN_UPDATE_METHOD_DIALOG = 1;
    public static final int PLUGIN_UPDATE_METHOD_AUTO = 2;
    public static final int PLUGIN_UPDATE_METHOD_QUIET = 3;
    public static final int PLUGIN_UPDATE_METHOD_DEFAULT = PLUGIN_UPDATE_ASK_FOR_METHOD;

    public static final String RECHECK_FILES_ON_START = "recheckFilesOnStart";
    public static final boolean RECHECK_FILES_ON_START_DEFAULT = false;

    public static final String SHOW_TEXT_TOOLBAR = "showButtonTextToolbar";
    public static final boolean SHOW_TEXT_TOOLBAR_DEFAULT = true;

    public static final String SHOW_PAYPAL = "showPaypal";
    public static final boolean SHOW_PAYPAL_DEFAULT = true;

    public static final String USE_SYSTEM_PROXIES = "useSystemProxies";
    public static final boolean USE_SYSTEM_PROXIES_DEFAULT = false;

    public static final String TRIM_DESCRIPTION_FOR_FILES = "trimDescription";
    public static final boolean TRIM_DESCRIPTION_FOR_FILES_DEFAULT = true;

    public static final String REMOVE_COMPLETED_DECRYPTER = "removeCompletedCrypter";
    public static final boolean REMOVE_COMPLETED_DECRYPTER_DEFAULT = true;

    public static final String DONT_ADD_NOTSUPPORTED_FROMCRYPTER = "dontAddNotSupportedFromCrypter";
    public static final boolean DONT_ADD_NOTSUPPORTED_FROMCRYPTER_DEFAULT = true;

    public static final String AVG_SPEED_MEASURED_SECONDS = "avgSpeedMeasuredSeconds";
    public static final int AVG_SPEED_MEASURED_SECONDS_DEFAULT = 10;

    public static final String MAX_RECENT_PHRASES_COUNT = "maxRecentPhrasesCount";
    public static final int MAX_RECENT_PHRASES_COUNT_DEFAULT = 6;

    public static final String HELP_URL = "HELP_URL";

    public static final String SPEED_LIMIT = "speedLimit";
    public static final int SPEED_LIMIT_DEFAULT = 250;

    public static final String SPEED_LIMIT_ENABLED = "speedLimitEnabled";
    public static final boolean SPEED_LIMIT_ENABLED_DEFAULT = false;

    public static final String SPEED_LIMIT_SPEEDS = "speedLimitSpeeds";
    public static final String SPEED_LIMIT_SPEEDS_DEFAULT = "0,,10,15,20,30,40,50,100,150,200,300";


    public static final String GLOBAL_SPEED_SLIDER_MIN = "globalSpeedMin";
    public static final int GLOBAL_SPEED_SLIDER_MIN_DEFAULT = 50;

    public static final String GLOBAL_SPEED_SLIDER_MAX = "globalSpeedMax";
    public static final int GLOBAL_SPEED_SLIDER_MAX_DEFAULT = 250;

    public static final String GLOBAL_SPEED_SLIDER_STEP = "globalSpeedSliderStep";
    public static final int GLOBAL_SPEED_SLIDER_STEP_DEFAULT = 10;

    public static final String SEARCH_ON_TYPE = "searchOnType";
    public static final boolean SEARCH_ON_TYPE_DEFAULT = true;


    public static final boolean SHOW_COMPLETED_DEFAULT = true;

    public static final String OPTIMIZE_SAVING_LIST = "optimizeSavingList";
    public static final boolean OPTIMIZE_SAVING_LIST_DEFAULT = false;

    public static final String CHECK_FOR_NEW_VERSION_TIME = "checkForNewVersionLastTime";
    public static final int CHECK_FOR_NEW_VERSION_TIME_DEFAULT = 0;

    public static final String TRAY_ICON_AUTOIMAGESIZE = "trayIconAutoImageSize";
    public static final boolean TRAY_ICON_AUTOIMAGESIZE_DEFAULT = true;

    public static final String MIN_DISK_SPACE = "minDiskSpaceMB";
    public static final int MIN_DISK_SPACE_DEFAULT = 30;

    public static final String AUTODETECT_SOCKSPROXY = "autodetectSOCKSProxy";
    public static final boolean AUTODETECT_SOCKSPROXY_DEFAULT = true;

    public static final String BLIND_MODE = "blindMode";
    public static final boolean BLIND_MODE_DEFAULT = false;

    public static final String DEFAULT_CONNECTION_SOCKS = "defaultConnectionSocks";
    public static final boolean DEFAULT_CONNECTION_SOCKS_DEFAULT = false;

    public static final String CANCEL_ON_ESCAPE = "cancelOnEscape";
    public static final boolean CANCEL_ON_ESCAPE_DEFAULT = true;

    public static final String TABLE_SORTABLE = "tableSortable";
    public static final boolean TABLE_SORTABLE_DEFAULT = true;


    public static final String DETECT_BLIND_MODE = "detectBlindMode";
    public static final boolean DETECT_BLIND_MODE_DEFAULT = false;

    public static final boolean SHOW_STATUSBAR_DEFAULT = true;
    public static final String SHOW_STATUSBAR = "showStatusbar";
    public static final String SHOW_TOOLBAR = "showToolbar";


    public static final boolean SHOW_TOOLBAR_DEFAULT = true;

    public static final String CONNECTION_TIMEOUT = "connectionTimeoutMS";
    public static final int CONNECTION_TIMEOUT_DEFAULT = 120 * 1000;

    public static final String ENABLE_NEW_LINK_CHECK_DOWNLOAD_HISTORY = "enableNewLinkCheckDownloadHistory";
    public static final boolean ENABLE_NEW_LINK_CHECK_DOWNLOAD_HISTORY_DEFAULT = false;

    public static final String ENABLE_DIRECT_DOWNLOADS = "enableDirectDownloads";
    public static final boolean ENABLE_DIRECT_DOWNLOADS_DEFAULT = false;

    public static final String ENABLE_CLIPBOARD_MONITORING_FOR_DIRECT_DOWNLOADS = "enableClipboardMonitoringForDirectDownloads";
    public static final boolean ENABLE_CLIPBOARD_MONITORING_FOR_DIRECT_DOWNLOADS_DEFAULT = false;

    public static final String REMOVE_NOT_SUPPORTED_PLUGINS = "removeNotSupportedPlugins";
    public static final boolean REMOVE_NOT_SUPPORTED_PLUGINS_DEFAULT = false;

    public static final String AUTO_START_DOWNLOADS_FROM_DECRYPTER = "autoStartDownloadsFromDecrypter";
    public static final boolean AUTO_START_DOWNLOADS_FROM_DECRYPTER_DEFAULT = true;

    public static final String TABLE_COLUMNS_RESIZE = "tableColumnsResize";
    public static final int TABLE_COLUMNS_RESIZE_DEFAULT = javax.swing.JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS;

    public static final String SEARCH_FIELD_TEXT = "searchFieldText";
    public static final String SEARCH_FIELD_SEARCH_ENGINE = "searchFieldSearchEngine";
    public static final String SEARCH_FIELD_SEARCH_ENGINE_DEFAULT = "google";
    public static final String SEARCH_FIELD_VISIBLE = "searchFieldVisible";
    public static final boolean SEARCH_FIELD_VISIBLE_DEFAULT = true;
    public static final String SEARCH_FIELD_WIDTH = "searchFieldWidth";
    public static final int SEARCH_FIELD_WIDTH_DEFAULT = 165;

    public static final String SHOW_PAYPAL_REQUEST = "showPaypalRequest";

    public static final String USE_RECYCLE_BIN = "useRecycleBin";
    public static final boolean USE_RECYCLE_BIN_DEFAULT = false;

    public static final String SEARCH_SUBTITLES_ENABLED = "searchSubtitlesEnabled";
    public static final boolean SEARCH_SUBTITLES_ENABLED_DEFAULT = false;

    public static final String SEARCH_SUBTITLES_LANGUAGE = "searchSubtitlesLanguage";

    public static final String RECONNECT_SCRIPT = "reconnectScript";
    public static final String RECONNECT_SCRIPT_ENABLED = "reconnectScriptEnabled";
    public static final boolean RECONNECT_SCRIPT_ENABLED_DEFAULT = true;

    public static final String QUIET_MODE_ENABLED = "quietModeEnabled";
    public static final boolean QUIET_MODE_ENABLED_DEFAULT = false;
    public static final String QUIET_MODE_ACTIVATION_MODE = "quietModeActivationMode";
    public static final String QUIET_MODE_ACTIVATION_ALWAYS = "always";
    public static final String QUIET_MODE_ACTIVATION_WHEN_WINDOWS_FOUND = "whenWindowsFound";
    public static final String QUIET_MODE_ACTIVATION_MODE_DEFAULT = QUIET_MODE_ACTIVATION_ALWAYS;
    public static final String QUIET_MODE_ACTIVATION_STRINGS = "quietModeActivationStrings";
    public static final String QUIET_MODE_ACTIVATION_STRINGS_DEFAULT = "VLC media player|iTunes|Call of Duty|- SMPlayer";
    public static final String QUIET_MODE_CASE_SENSITIVE_SEARCH = "quietModeCaseSensitiveSearch";
    public static final boolean QUIET_MODE_CASE_SENSITIVE_SEARCH_DEFAULT = false;
    public static final String QUIET_MODE_NO_SOUNDS = "quietModeNoSounds";
    public static final boolean QUIET_MODE_NO_SOUNDS_DEFAULT = true;
    public static final String QUIET_MODE_NO_CAPTCHA = "quietModeNoCaptcha";
    public static final boolean QUIET_MODE_NO_CAPTCHA_DEFAULT = true;
    public static final String QUIET_MODE_NO_CONFIRM_DIALOGS = "quietModeNoConfirmDialogs";
    public static final boolean QUIET_MODE_NO_CONFIRM_DIALOGS_DEFAULT = true;
    public static final String QUIET_MODE_PLAY_SOUND_ON_ACTIVATE = "quietModePlaySoundOnActivate";
    public static final boolean QUIET_MODE_PLAY_SOUND_ON_ACTIVATE_DEFAULT = false;

    public static final String PREVENT_STANDBY_WHILE_DOWNLOADING = "preventStandbyWhileDownloading";
    public static final boolean PREVENT_STANDBY_WHILE_DOWNLOADING_DEFAULT = false;

    public static final String POP_WINDOW_WITHOUT_MAIN_WINDOW_IN_QUIET_MODE = "popWindowWithoutMainFrame";
    public static final boolean POP_WINDOW_WITHOUT_MAIN_WINDOW_IN_QUIET_MODE_DEFAULT = false;

    public static final String CHAR_ENCODING_FOR_PROPERTIES_FILES = "charEncodingForPropertiesFiles";

    public static final String OPEN_INCOMPLETE_FILES = "openIncompleteFiles";
    public static final boolean OPEN_INCOMPLETE_FILES_DEFAULT = false;

    public static final String RENAME_FILE_ACTION_SELECT_WITHOUT_EXTENSION = "renameFileActionSelectWithoutExtension";
    public static final boolean RENAME_FILE_ACTION_SELECT_WITHOUT_EXTENSION_DEFAULT = true;

    /**
     * See http://wordrider.net/forum/7/9889/_bug__report_-_slow_directory_chooser
     */
    public static final String SELECT_DIR_DIALOG_OVERRIDE = "selectDirDialogOverride";
    public static final boolean SELECT_DIR_DIALOG_OVERRIDE_DEFAULT = false;

    public static final String ZOOM_CAPTCHA_IMAGE = "zoomCaptchaImage";
    public static final boolean ZOOM_CAPTCHA_IMAGE_DEFAULT = false;

    public static final String MAX_SIMULTANEOUS_RUN_CHECK = "maxSimultaneousRunCheck";
    public static final int MAX_SIMULTANEOUS_RUN_CHECK_DEFAULT = 5;

    public static final String CONTENT_TABLE_DATE_FORMAT = "contentTableDateFormat";
    public static final String HISTORY_TABLE_DATE_FORMAT = "historyTableDateFormat";

    public static final String SKIP_DUPLICATE_FILES = "skipDuplicateFiles";
    public static final boolean SKIP_DUPLICATE_FILES_DEFAULT = false;

    public static final String PLUGIN_WITH_PRIORITY_PRECEDENCE = "pluginWithPriorityPrecedence";
    public static final boolean PLUGIN_WITH_PRIORITY_PRECEDENCE_DEFAULT = false;

    public static final String USE_PROXY_FOR_PLUGIN = "useProxyForPlugin";
    public static final boolean USE_PROXY_FOR_PLUGIN_DEFAULT = false;

    public static final String CA_CERT_URL = "caCertUrl";
    public static final String SSL_VERIFY_HOSTNAME = "sslVerifyHostName";
    public static final boolean SSL_VERIFY_HOSTNAME_DEFAULT = false;

}





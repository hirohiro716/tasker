package com.hirohiro716.desktop.task.config;

import com.hirohiro716.scent.property.PropertyInterface;

/**
 * 設定のプロパティのクラス。
 */
public enum ConfigProperty implements PropertyInterface {
    WINDOW_LOCATION_X("ウィンドウ位置x", "0"),
    WINDOW_LOCATION_Y("ウィンドウ位置y", "0"),
    TASKS("タスク", "[]"),
    ;

    /**
     * コンストラクタ。論理名、初期値、最大文字数を指定する。
     * 
     * @param logicalName
     * @param defaultValue
     * @param maximumLength
     */
    private ConfigProperty(String logicalName, Object defaultValue, int maximumLength) {
        this.logicalName = logicalName;
        this.defaultValue = defaultValue;
        this.maximumLength = maximumLength;
    }

    /**
     * コンストラクタ。論理名、初期値を指定する。
     * 
     * @param logicalName
     * @param defaultValue
     */
    private ConfigProperty(String logicalName, Object defaultValue) {
        this(logicalName, defaultValue, -1);
    }

    /**
     * コンストラクタ。論理名を指定する。
     * 
     * @param logicalName
     */
    private ConfigProperty(String logicalName) {
        this(logicalName, null, -1);
    }

    private String logicalName;

    @Override
    public String getLogicalName() {
        return this.logicalName;
    }

    private Object defaultValue;

    @Override
    public Object getDefaultValue() {
        return this.defaultValue;
    }

    private int maximumLength;

    @Override
    public int getMaximumLength() {
        return this.maximumLength;
    }
}

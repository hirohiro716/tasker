package com.hirohiro716.desktop.task;

import com.hirohiro716.scent.datetime.Datetime;
import com.hirohiro716.scent.property.PropertyInterface;

/**
 * タスクのプロパティのクラス。
 */
public enum TaskProperty implements PropertyInterface {
    ID("ID"),
    DESCRIPTION("説明"),
    DIRECTORY("ディレクトリ"),
    COMPLETED_TIME("完了日時"),
    CREATED_TIME("作成日時", Datetime.newInstance().toString()),
    SORT("ソート", 9999),
    ;

    /**
     * コンストラクタ。論理名、初期値、最大文字数を指定する。
     * 
     * @param logicalName
     * @param defaultValue
     * @param maximumLength
     */
    private TaskProperty(String logicalName, Object defaultValue, int maximumLength) {
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
    private TaskProperty(String logicalName, Object defaultValue) {
        this(logicalName, defaultValue, -1);
    }

    /**
     * コンストラクタ。論理名を指定する。
     * 
     * @param logicalName
     */
    private TaskProperty(String logicalName) {
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

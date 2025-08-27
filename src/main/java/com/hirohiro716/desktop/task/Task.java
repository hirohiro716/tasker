package com.hirohiro716.desktop.task;

import java.util.Date;

import com.hirohiro716.desktop.task.config.Config;
import com.hirohiro716.scent.StringObject;
import com.hirohiro716.scent.datetime.Datetime;
import com.hirohiro716.scent.io.json.JSONObject;
import com.hirohiro716.scent.io.json.ParseException;

/**
 * タスクのクラス。
 */
public class Task extends JSONObject {

    /**
     * コンストラクタ。 JSON定義文と設定を指定する。
     * 
     * @param json
     * @param config
     * @throws ParseException
     */
    public Task(String json, Config config) throws ParseException {
        super(json);
        this.config = config;
    }

    /**
     * コンストラクタ。 設定を指定する。
     * 
     * @param config
     * @throws ParseException
     */
    public Task(Config config) throws ParseException {
        this("{}", config);
        for (TaskProperty property : TaskProperty.values()) {
            Object value = null;
            switch (property) {
                case ID:
                    StringObject id = new StringObject();
                    while (id.length() == 0 || this.config.existsTask(id.toString())) {
                        id.set(Datetime.newInstance().getAllMilliSecond());
                    }
                    value = id.toString();
                    break;
                case DESCRIPTION:
                case DIRECTORY:
                case COMPLETED_TIME:
                case CREATED_TIME:
                    value = StringObject.newInstance(property.getDefaultValue());
                    break;
                case DESCRIPTION_IS_READONLY:
                case SORT:
                    value = property.getDefaultValue();
                    break;
            }
            this.put(property, value);
        }
    }

    private Config config;
    
    /**
     * このタスクのIDを取得する。
     * 
     * @return
     */
    public long getID() {
        StringObject value = new StringObject(0);
        if (this.isNull(TaskProperty.ID) == false) {
            value.append(this.get(TaskProperty.ID).getContent());
        }
        return value.toLong();
    }

    /**
     * このタスクの詳細を取得する。
     * 
     * @return
     */
    public String getDescription() {
        StringObject value = new StringObject();
        if (this.isNull(TaskProperty.DESCRIPTION) == false) {
            value.append(this.get(TaskProperty.DESCRIPTION).getContent());
        }
        return value.toString();
    }

    /**
     * このタスクの詳細が読み取り専用の場合はtrueを返す。
     * 
     * @return
     */
    public boolean getDescriptionIsReadonly() {
        if (this.isNull(TaskProperty.DESCRIPTION_IS_READONLY) == false) {
            return (boolean) this.get(TaskProperty.DESCRIPTION_IS_READONLY).getContent();
        }
        return false;
    }

    /**
     * このタスクのディレクトリを取得する。
     * 
     * @return
     */
    public String getDirectory() {
        StringObject value = new StringObject();
        if (this.isNull(TaskProperty.DIRECTORY) == false) {
            value.append(this.get(TaskProperty.DIRECTORY).getContent());
        }
        return value.toString();
    }

    /**
     * このタスクの完了日時を取得する。
     * 
     * @return
     */
    public Datetime getCompletedTime() {
        StringObject value = new StringObject();
        if (this.isNull(TaskProperty.COMPLETED_TIME) == false) {
            value.append(this.get(TaskProperty.COMPLETED_TIME).getContent());
        }
        if (value.toLong() == null) {
            return null;
        }
        return new Datetime(new Date(value.toLong()));
    }

    /**
     * このタスクのソートを取得する。
     * 
     * @return
     */
    public long getSort() {
        StringObject value = new StringObject();
        if (this.isNull(TaskProperty.SORT) == false) {
            value.append(this.get(TaskProperty.SORT).getContent());
        }
        if (value.length() == 0) {
            value.append(TaskProperty.SORT.getDefaultValue());
        }
        return value.toLong();
    }
}

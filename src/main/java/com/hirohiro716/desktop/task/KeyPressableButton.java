package com.hirohiro716.desktop.task;

import org.gnome.gdk.EventKey;
import org.gnome.gdk.Keyval;
import org.gnome.gtk.Widget;

/**
 * キーで押せるボタンのクラス。
 */
public class KeyPressableButton extends org.gnome.gtk.Button {
    
    /**
     * コンストラクタ。
     * 
     * @param text
     */
    public KeyPressableButton(String text) {
        super(text);
        this.connect(this.keyPressEvent);
    }

    /**
     * コンストラクタ。
     */
    public KeyPressableButton() {
        super();
        this.connect(this.keyPressEvent);
    }

    /**
     * キー押下時のイベントハンドラー。
     */
    private Widget.KeyPressEvent keyPressEvent = new Widget.KeyPressEvent() {

        @Override
        public boolean onKeyPressEvent(Widget widget, EventKey eventKey) {
            KeyPressableButton button = KeyPressableButton.this;
            if (eventKey.getKeyval() != Keyval.Space) {
                return false;
            }
            button.emitClicked();
            return true;
        }
    };
}

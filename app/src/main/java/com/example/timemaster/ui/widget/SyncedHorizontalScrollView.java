package com.example.timemaster.ui.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.HorizontalScrollView;

/** HorizontalScrollView có thể “cặp đôi” để cuộn đồng bộ (kể cả khi fling, nhưng không gọi fling của partner). */
public class SyncedHorizontalScrollView extends HorizontalScrollView {

    private SyncedHorizontalScrollView partner;
    private boolean isSyncing = false;

    public SyncedHorizontalScrollView(Context context) { super(context); }
    public SyncedHorizontalScrollView(Context context, AttributeSet attrs) { super(context, attrs); }
    public SyncedHorizontalScrollView(Context context, AttributeSet attrs, int defStyleAttr) { super(context, attrs, defStyleAttr); }

    public void setPartner(SyncedHorizontalScrollView partner) {
        this.partner = partner;
    }

    @Override
    protected void onScrollChanged(int l, int t, int oldl, int oldt) {
        super.onScrollChanged(l, t, oldl, oldt);
        if (partner == null || isSyncing) return;
        isSyncing = true;
        // Chỉ đồng bộ vị trí; KHÔNG gọi fling() partner để tránh vòng lặp.
        partner.scrollTo(l, t);
        isSyncing = false;
    }

    @Override
    public void fling(int velocityX) {
        // Gọi fling bình thường cho view hiện tại.
        // KHÔNG gọi partner.fling(...) để tránh vòng lặp đệ quy.
        super.fling(velocityX);
        // partner sẽ được kéo theo nhờ onScrollChanged() ở trên.
    }
}

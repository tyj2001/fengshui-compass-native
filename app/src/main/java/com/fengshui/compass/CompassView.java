package com.fengshui.compass;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.View;

/**
 * 风水罗盘盘面绘制View
 * 包含：24山、八卦、天干地支、360度刻度、指针
 */
public class CompassView extends View {

    private float azimuth = 0f; // 当前方位角度

    // 颜色定义
    private static final int COLOR_BG = 0xFF1A1A2E;
    private static final int COLOR_RING_OUTER = 0xFFD4A574;
    private static final int COLOR_RING_INNER = 0xFF2D2D44;
    private static final int COLOR_TEXT_GOLD = 0xFFD4A574;
    private static final int COLOR_TEXT_RED = 0xFFE74C3C;
    private static final int COLOR_TEXT_WHITE = 0xFFFFFFFF;
    private static final int COLOR_NEEDLE_RED = 0xFFE74C3C;
    private static final int COLOR_NEEDLE_BLACK = 0xFF2C3E50;
    private static final int COLOR_CENTER_DOT = 0xFFD4A574;
    private static final int COLOR_TICK = 0xFFD4A574;
    private static final int COLOR_BAGUA_BG = 0xFF3D3D55;

    private Paint paint;
    private Paint textPaint;
    private Paint tickPaint;
    private Paint needlePaint;
    private Paint baguaPaint;

    private float centerX, centerY, radius;
    private float density;

    // ========== 24山数据 ==========
    // 顺序：从正北（0度）顺时针排列
    private static final String[] MOUNTAINS_24 = {
        "子", "癸", "丑", "艮", "寅", "甲",
        "卯", "乙", "辰", "巽", "巳", "丙",
        "午", "丁", "未", "坤", "申", "庚",
        "酉", "辛", "戌", "乾", "亥", "壬"
    };

    // 24山对应的角度（中心角度）
    private static final float[] MOUNTAIN_ANGLES = {
        0f, 15f, 30f, 45f, 60f, 75f,
        90f, 105f, 120f, 135f, 150f, 165f,
        180f, 195f, 210f, 225f, 240f, 255f,
        270f, 285f, 300f, 315f, 330f, 345f
    };

    // ========== 八卦 ==========
    // 后天八卦方位（从正北顺时针）
    private static final String[] BAGUA = {
        "坎", "艮", "震", "巽", "离", "坤", "兑", "乾"
    };
    // 后天八卦对应角度
    private static final float[] BAGUA_ANGLES = {
        0f, 45f, 90f, 135f, 180f, 225f, 270f, 315f
    };

    // ========== 十二地支 ==========
    private static final String[] DIZHI = {
        "子", "丑", "寅", "卯", "辰", "巳",
        "午", "未", "申", "酉", "戌", "亥"
    };

    // ========== 十天干 ==========
    private static final String[] TIANGAN = {
        "甲", "乙", "丙", "丁", "戊", "己", "庚", "辛", "壬", "癸"
    };

    public CompassView(Context context) {
        super(context);
        init();
    }

    public CompassView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        density = getResources().getDisplayMetrics().density;

        paint = new Paint(Paint.ANTI_ALIAS_FLAG);

        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTypeface(Typeface.DEFAULT_BOLD);

        tickPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        needlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        baguaPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        baguaPaint.setStyle(Paint.Style.FILL);
    }

    /**
     * 更新方位角
     */
    public void setAzimuth(float azimuth) {
        this.azimuth = azimuth;
        invalidate();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        centerX = w / 2f;
        centerY = h / 2f;
        // 留出边距
        float padding = 20 * density;
        radius = Math.min(centerX, centerY) - padding;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // 背景
        canvas.drawColor(COLOR_BG);

        // 绘制罗盘（盘面不动，指针旋转）
        drawOuterRing(canvas);
        drawDegreeTicks(canvas);
        drawBagua(canvas);
        drawMountains24(canvas);
        drawCenterCircle(canvas);
        drawNeedle(canvas);
        drawDirectionText(canvas);
    }

    /**
     * 绘制外圈装饰环
     */
    private void drawOuterRing(Canvas canvas) {
        // 外圈金色边框
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(3 * density);
        paint.setColor(COLOR_RING_OUTER);
        canvas.drawCircle(centerX, centerY, radius, paint);

        // 内圈
        paint.setStrokeWidth(1.5f * density);
        paint.setColor(COLOR_RING_INNER);
        canvas.drawCircle(centerX, centerY, radius - 15 * density, paint);
    }

    /**
     * 绘制360度刻度线
     */
    private void drawDegreeTicks(Canvas canvas) {
        float outerR = radius - 8 * density;
        float innerR;

        for (int i = 0; i < 360; i++) {
            float angle = (float) Math.toRadians(i);

            if (i % 15 == 0) {
                // 15度（24山刻度）— 长线
                tickPaint.setStrokeWidth(3 * density);
                tickPaint.setColor(COLOR_TEXT_GOLD);
                innerR = radius - 30 * density;
            } else if (i % 5 == 0) {
                // 5度 — 中线
                tickPaint.setStrokeWidth(2 * density);
                tickPaint.setColor(0xFFA08060);
                innerR = radius - 22 * density;
            } else {
                // 1度 — 短线
                tickPaint.setStrokeWidth(1 * density);
                tickPaint.setColor(0xFF605040);
                innerR = radius - 16 * density;
            }

            float startX = centerX + outerR * (float) Math.sin(angle);
            float startY = centerY - outerR * (float) Math.cos(angle);
            float endX = centerX + innerR * (float) Math.sin(angle);
            float endY = centerY - innerR * (float) Math.cos(angle);

            canvas.drawLine(startX, startY, endX, endY, tickPaint);
        }

        // 360度数字标注（每30度）
        textPaint.setTextSize(11 * density);
        textPaint.setColor(COLOR_TEXT_GOLD);
        float textR = radius - 38 * density;
        for (int i = 0; i < 360; i += 30) {
            float rad = (float) Math.toRadians(i);
            float tx = centerX + textR * (float) Math.sin(rad);
            float ty = centerY - textR * (float) Math.cos(rad) + 4 * density;
            canvas.drawText(String.valueOf(i), tx, ty, textPaint);
        }
    }

    /**
     * 绘制八卦
     * 后天八卦方位
     */
    private void drawBagua(Canvas canvas) {
        float baguaR = radius - 50 * density;
        textPaint.setTextSize(18 * density);

        for (int i = 0; i < BAGUA.length; i++) {
            float rad = (float) Math.toRadians(BAGUA_ANGLES[i]);
            float bx = centerX + baguaR * (float) Math.sin(rad);
            float by = centerY - baguaR * (float) Math.cos(rad);

            // 八卦背景圆
            float dotR = 14 * density;
            baguaPaint.setColor(COLOR_BAGUA_BG);
            canvas.drawCircle(bx, by, dotR, baguaPaint);

            // 八卦文字
            // 坎(水)=黑、离(火)=红，其他为金色
            if (i == 0) textPaint.setColor(Color.BLACK);
            else if (i == 4) textPaint.setColor(COLOR_TEXT_RED);
            else textPaint.setColor(COLOR_TEXT_GOLD);

            canvas.drawText(BAGUA[i], bx, by + 6 * density, textPaint);
        }
    }

    /**
     * 绘制24山
     */
    private void drawMountains24(Canvas canvas) {
        // 24山文字 — 外圈
        textPaint.setColor(COLOR_TEXT_GOLD);
        textPaint.setTextSize(16 * density);

        // 24山文字 — 中圈（天干/地支）
        float outerR = radius - 68 * density;
        float innerR = radius - 90 * density;

        for (int i = 0; i < 24; i++) {
            float rad = (float) Math.toRadians(MOUNTAIN_ANGLES[i]);

            // 外圈：24山主字
            float ox = centerX + outerR * (float) Math.sin(rad);
            float oy = centerY - outerR * (float) Math.cos(rad) + 5 * density;

            // 子午卯酉（正北正南正东正西）用红色
            if (i == 0 || i == 6 || i == 12 || i == 18) {
                textPaint.setColor(COLOR_TEXT_RED);
                textPaint.setTextSize(18 * density);
            } else {
                textPaint.setColor(COLOR_TEXT_GOLD);
                textPaint.setTextSize(16 * density);
            }
            canvas.drawText(MOUNTAINS_24[i], ox, oy, textPaint);

            // 内圈：对应地支/天干标注
            textPaint.setTextSize(11 * density);
            textPaint.setColor(0xFFA09070);
            String subText = getSubText(i);
            float ix = centerX + innerR * (float) Math.sin(rad);
            float iy = centerY - innerR * (float) Math.cos(rad) + 4 * density;
            canvas.drawText(subText, ix, iy, textPaint);
        }
    }

    /**
     * 获取24山对应的辅助标注
     */
    private String getSubText(int index) {
        // 子(0) -> 子(地支), 癸(1) -> 癸(天干), 丑(2) -> 丑(地支) ...
        // 24山中：地支8个（子丑寅卯辰巳午未申酉戌亥），天干8个（甲乙丙丁庚辛壬癸），四维4个（乾坤艮巽）
        String name = MOUNTAINS_24[index];
        // 检查是否在地支中
        for (String dz : DIZHI) {
            if (dz.equals(name)) return dz;
        }
        // 检查是否在天干中
        for (String tg : TIANGAN) {
            if (tg.equals(name)) return tg;
        }
        // 四维：乾坤艮巽
        return name;
    }

    /**
     * 绘制中心太极圈
     */
    private void drawCenterCircle(Canvas canvas) {
        float centerR = 20 * density;

        // 外圈
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(2 * density);
        paint.setColor(COLOR_RING_OUTER);
        canvas.drawCircle(centerX, centerY, centerR + 4 * density, paint);

        // 内圆背景
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(0xFF2D2D44);
        canvas.drawCircle(centerX, centerY, centerR, paint);

        // 中心小圆点
        paint.setColor(COLOR_CENTER_DOT);
        canvas.drawCircle(centerX, centerY, 4 * density, paint);
    }

    /**
     * 绘制指南针指针（根据方位角旋转）
     */
    private void drawNeedle(Canvas canvas) {
        float needleLen = radius - 55 * density;
        float needleBase = 20 * density;
        float needleWidth = 8 * density;

        canvas.save();
        // 指针旋转：方位角是顺时针，Canvas旋转也是顺时针
        canvas.rotate(azimuth, centerX, centerY);

        // 红色指针（南 — 指向当前方位）
        needlePaint.setColor(COLOR_NEEDLE_RED);
        Path southPath = new Path();
        southPath.moveTo(centerX - needleWidth, centerY + needleBase);
        southPath.lineTo(centerX, centerY - needleLen);
        southPath.lineTo(centerX + needleWidth, centerY + needleBase);
        southPath.close();
        canvas.drawPath(southPath, needlePaint);

        // 黑色指针（北 — 指向相反方向）
        needlePaint.setColor(COLOR_NEEDLE_BLACK);
        Path northPath = new Path();
        northPath.moveTo(centerX - needleWidth, centerY - needleBase);
        northPath.lineTo(centerX, centerY + needleLen);
        northPath.lineTo(centerX + needleWidth, centerY - needleBase);
        northPath.close();
        canvas.drawPath(northPath, needlePaint);

        // 指针中心覆盖
        needlePaint.setColor(COLOR_CENTER_DOT);
        canvas.drawCircle(centerX, centerY, 6 * density, needlePaint);

        canvas.restore();
    }

    /**
     * 绘制当前方位文字信息
     */
    private void drawDirectionText(Canvas canvas) {
        // 顶部显示当前方位角度和24山
        textPaint.setColor(COLOR_TEXT_WHITE);
        textPaint.setTextSize(14 * density);
        textPaint.setTextAlign(Paint.Align.LEFT);

        // 计算当前指向的24山索引
        int mountainIndex = getCurrentMountainIndex();
        String mountainName = MOUNTAINS_24[mountainIndex];
        String directionName = getDirectionName(azimuth);

        String info = String.format("方位: %.1f°   %s山  %s",
                azimuth, mountainName, directionName);
        canvas.drawText(info, 20 * density, 35 * density, textPaint);

        // 底部显示子午线标注
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setColor(0xFF605040);
        textPaint.setTextSize(10 * density);
        canvas.drawText("子午线", centerX, getHeight() - 15 * density, textPaint);
    }

    /**
     * 获取当前方位对应的24山索引
     */
    private int getCurrentMountainIndex() {
        // 将方位角归一化到 0-360
        float angle = azimuth % 360;
        if (angle < 0) angle += 360;
        // 每个山占15度
        int index = (int) Math.round(angle / 15f) % 24;
        return index;
    }

    /**
     * 获取方位名称
     */
    private String getDirectionName(float degrees) {
        String[] directions = {"北", "东北", "东", "东南", "南", "西南", "西", "西北"};
        float[] ranges = {0, 45, 90, 135, 180, 225, 270, 315};
        String closest = directions[0];
        float minDiff = 360;
        for (int i = 0; i < ranges.length; i++) {
            float diff = Math.abs(degrees - ranges[i]);
            if (diff > 180) diff = 360 - diff;
            if (diff < minDiff) {
                minDiff = diff;
                closest = directions[i];
            }
        }
        return closest;
    }
}

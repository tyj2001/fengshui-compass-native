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
 * 专业风水罗盘盘面绘制View - 1:1复刻真实罗盘
 * 
 * 真实罗盘圈层结构（从外向内）：
 * 1. 外圈装饰边
 * 2. 二十八星宿（最外圈）
 * 3. 一百二十分金
 * 4. 六十甲子
 * 5. 天盘缝针（24山 +7.5°）
 * 6. 人盘中针（24山 -7.5°）
 * 7. 地盘正针（24山 0°）
 * 8. 六十四卦
 * 9. 先天八卦/后天八卦
 * 10. 天池（中心太极+指针）
 * 
 * 指针特点：磁针一端尖（指南），一端有角（指北）
 */
public class CompassView extends View {

    private float azimuth = 0f;

    // ========== 颜色定义 ==========
    private static final int COLOR_BG = 0xFF1A1A2E;           // 深蓝黑背景
    private static final int COLOR_RING_GOLD = 0xFFD4A574;    // 金色圈线
    private static final int COLOR_RING_DARK = 0xFF3D3D55;    // 深色圈线
    private static final int COLOR_TEXT_GOLD = 0xFFD4A574;    // 金色文字
    private static final int COLOR_TEXT_RED = 0xFFE74C3C;     // 红色文字（子午卯酉）
    private static final int COLOR_TEXT_BLUE = 0xFF6B9BD1;    // 蓝色（天盘）
    private static final int COLOR_TEXT_GREEN = 0xFF7CB87C;   // 绿色（人盘）
    private static final int COLOR_TEXT_WHITE = 0xFFFFFFFF;
    private static final int COLOR_TEXT_GRAY = 0xFF9090A0;    // 灰色辅助文字
    private static final int COLOR_NEEDLE_SOUTH = 0xFFE74C3C; // 红针（南/尖头）
    private static final int COLOR_NEEDLE_NORTH = 0xFF2C3E50; // 黑针（北/有角）
    private static final int COLOR_CENTER_BG = 0xFF2A2A40;   // 天池背景
    private static final int COLOR_RED_LINE = 0xFFE74C3C;     // 天池底部红线
    private static final int COLOR_BAGUA_BG = 0xFF252540;    // 八卦背景

    // ========== 画笔 ==========
    private Paint paint;
    private Paint textPaint;
    private Paint tickPaint;
    private Paint needlePaint;
    private Paint sectorPaint;

    // ========== 尺寸相关 ==========
    private float centerX, centerY, radius;
    private float density;

    // ========== 24山数据 ==========
    private static final String[] MOUNTAINS_24 = {
        "壬", "子", "癸", "丑", "艮", "寅",
        "甲", "卯", "乙", "辰", "巽", "巳",
        "丙", "午", "丁", "未", "坤", "申",
        "庚", "酉", "辛", "戌", "乾", "亥"
    };

    // 24山中心角度
    private static final float[] MOUNTAIN_ANGLES = {
        345f, 0f, 15f, 30f, 45f, 60f,
        75f, 90f, 105f, 120f, 135f, 150f,
        165f, 180f, 195f, 210f, 225f, 240f,
        255f, 270f, 285f, 300f, 315f, 330f
    };

    // ========== 后天八卦 ==========
    private static final String[] BAGUA = {
        "坎", "艮", "震", "巽", "离", "坤", "兑", "乾"
    };
    private static final float[] BAGUA_ANGLES = {
        0f, 45f, 90f, 135f, 180f, 225f, 270f, 315f
    };

    // ========== 二十八星宿 ==========
    private static final String[] XIU_28 = {
        "角", "亢", "氐", "房", "心", "尾", "箕",
        "斗", "牛", "女", "虚", "危", "室", "壁",
        "奎", "娄", "胃", "昴", "毕", "觜", "参",
        "井", "鬼", "柳", "星", "张", "翼", "轸"
    };

    // 二十八星宿中心角度（每宿12.857度，从特定位置开始）
    private static final float[] XIU_ANGLES;
    static {
        XIU_ANGLES = new float[28];
        float startAngle = 112.5f;  // 角宿起始位置
        float step = 360f / 28f;
        for (int i = 0; i < 28; i++) {
            float angle = startAngle + i * step;
            if (angle >= 360) angle -= 360;
            if (angle < 0) angle += 360;
            XIU_ANGLES[i] = angle;
        }
    }

    // ========== 六十甲子 ==========
    private static final String[] SEXAGENARY = {
        "甲子", "乙丑", "丙寅", "丁卯", "戊辰", "己巳", "庚午", "辛未", "壬申", "癸酉",
        "甲戌", "乙亥", "丙子", "丁丑", "戊寅", "己卯", "庚辰", "辛巳", "壬午", "癸未",
        "甲申", "乙酉", "丙戌", "丁亥", "戊子", "己丑", "庚寅", "辛卯", "壬辰", "癸巳",
        "甲午", "乙未", "丙申", "丁酉", "戊戌", "己亥", "庚子", "辛丑", "壬寅", "癸卯",
        "甲辰", "乙巳", "丙午", "丁未", "戊申", "己酉", "庚戌", "辛亥", "壬子", "癸丑",
        "甲寅", "乙卯", "丙辰", "丁巳", "戊午", "己未", "庚申", "辛酉", "壬戌", "癸亥"
    };

    // ========== 六十四卦 ==========
    private static final String[] HEXAGRAM_64 = {
        "乾", "坤", "屯", "蒙", "需", "讼", "师", "比",
        "小畜", "履", "泰", "否", "同人", "大有", "谦", "豫",
        "随", "蛊", "临", "观", "噬嗑", "贲", "剥", "复",
        "无妄", "大畜", "颐", "大过", "坎", "离",
        "咸", "恒", "遁", "大壮", "晋", "明夷", "家人", "睽",
        "蹇", "解", "损", "益", "夬", "姤", "萃", "升",
        "困", "井", "革", "鼎", "震", "艮", "渐", "归妹",
        "丰", "旅", "巽", "兑", "涣", "节", "中孚", "小过",
        "既济", "未济"
    };

    // ========== 分金天干 ==========
    private static final String[] FENJIN_LABELS = {
        "甲", "", "丙", "", "戊", "", "庚", "", "壬", "",
        "", "乙", "", "丁", "", "己", "", "辛", "", "癸"
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
        sectorPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    }

    public void setAzimuth(float azimuth) {
        this.azimuth = azimuth;
        invalidate();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        centerX = w / 2f;
        centerY = h / 2f;
        float padding = 8 * density;
        radius = Math.min(centerX, centerY) - padding;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawColor(COLOR_BG);

        // 绘制各圈层（从外向内）
        drawOuterBorder(canvas);           // 1. 外圈装饰
        drawOuterDecoration(canvas);        // 2. 外圈装饰细节
        draw28Xiu(canvas);                  // 3. 二十八星宿
        drawFenjin(canvas);                 // 4. 一百二十分金
        drawSexagenary(canvas);             // 5. 六十甲子
        drawTianPan(canvas);                // 6. 天盘缝针
        drawRenPan(canvas);                  // 7. 人盘中针
        drawDiPan(canvas);                   // 8. 地盘正针（24山主圈）
        drawHexagram(canvas);                // 9. 六十四卦
        drawBagua(canvas);                   // 10. 后天八卦
        drawDegreeScale(canvas);             // 11. 360度刻度（在外八卦和天池之间）
        drawTianchi(canvas);                 // 12. 天池（中心）
        drawNeedle(canvas);                  // 13. 指针
        drawInfo(canvas);                    // 14. 信息显示
    }

    // ========================================================================
    // 1. 外圈装饰
    // ========================================================================
    private void drawOuterBorder(Canvas canvas) {
        // 最外层粗金边
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(4 * density);
        paint.setColor(COLOR_RING_GOLD);
        canvas.drawCircle(centerX, centerY, radius, paint);

        // 第二层细金边
        paint.setStrokeWidth(1.5f * density);
        paint.setColor(COLOR_RING_DARK);
        canvas.drawCircle(centerX, centerY, radius - 3 * density, paint);
    }

    private void drawOuterDecoration(Canvas canvas) {
        // 绘制四正方向的红色三角标记
        float[] cardinalAngles = {0f, 90f, 180f, 270f};  // 北、东、南、西
        float r = radius - 1 * density;
        
        for (float angle : cardinalAngles) {
            float rad = (float) Math.toRadians(angle - 90);
            float x = centerX + r * (float) Math.cos(rad);
            float y = centerY + r * (float) Math.sin(rad);
            
            // 画小三角
            Path tri = new Path();
            tri.moveTo(x, y);
            float triSize = 6 * density;
            float rad1 = rad + (float) Math.toRadians(120);
            float rad2 = rad - (float) Math.toRadians(120);
            tri.lineTo(x + triSize * (float) Math.cos(rad1), y + triSize * (float) Math.sin(rad1));
            tri.lineTo(x + triSize * (float) Math.cos(rad2), y + triSize * (float) Math.sin(rad2));
            tri.close();
            
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(COLOR_TEXT_RED);
            canvas.drawPath(tri, paint);
        }
    }

    // ========================================================================
    // 3. 二十八星宿（最外圈）
    // ========================================================================
    private void draw28Xiu(Canvas canvas) {
        float ringWidth = 30 * density;
        float outerR = radius - 5 * density;
        float innerR = outerR - ringWidth;
        float textR = (outerR + innerR) / 2f;

        // 绘制背景
        float stepAngle = 360f / 28f;
        for (int i = 0; i < 28; i++) {
            float startAngle = XIU_ANGLES[i] - stepAngle / 2f;
            
            if (i % 2 == 0) {
                sectorPaint.setColor(0xFF252540);
            } else {
                sectorPaint.setColor(0xFF2A2A48);
            }
            
            RectF rect = new RectF(centerX - outerR, centerY - outerR, centerX + outerR, centerY + outerR);
            canvas.drawArc(rect, startAngle - 90, stepAngle, true, sectorPaint);
        }

        // 绘制分隔线
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(0.5f * density);
        paint.setColor(COLOR_RING_DARK);
        for (int i = 0; i < 28; i++) {
            float angle = (float) Math.toRadians(XIU_ANGLES[i] - stepAngle / 2f);
            float sx = centerX + innerR * (float) Math.sin(angle);
            float sy = centerY - innerR * (float) Math.cos(angle);
            float ex = centerX + outerR * (float) Math.sin(angle);
            float ey = centerY - outerR * (float) Math.cos(angle);
            canvas.drawLine(sx, sy, ex, ey, paint);
        }

        // 绘制星宿名（内侧）和七曜（外侧）
        textPaint.setTextSize(11 * density);
        for (int i = 0; i < 28; i++) {
            float rad = (float) Math.toRadians(XIU_ANGLES[i] - 90);
            
            // 星宿名
            textPaint.setColor(COLOR_TEXT_GOLD);
            float nx = centerX + textR * (float) Math.cos(rad);
            float ny = centerY + textR * (float) Math.sin(rad) + 4 * density;
            canvas.drawText(XIU_28[i], nx, ny, textPaint);
        }

        // 圈边界线
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(1 * density);
        paint.setColor(COLOR_RING_DARK);
        canvas.drawCircle(centerX, centerY, outerR, paint);
        canvas.drawCircle(centerX, centerY, innerR, paint);
    }

    // ========================================================================
    // 4. 一百二十分金
    // ========================================================================
    private void drawFenjin(Canvas canvas) {
        float ringWidth = 14 * density;
        float outerR = radius - 37 * density;
        float innerR = outerR - ringWidth;
        float textR = (outerR + innerR) / 2f;

        // 120个分金，每3度一个
        float step = 3f;
        textPaint.setTextSize(7 * density);

        for (int i = 0; i < 120; i++) {
            float angleDeg = i * step;
            float rad = (float) Math.toRadians(angleDeg - 90);
            
            // 只显示部分分金（避免太拥挤）
            int posInFive = i % 5;
            if (posInFive != 2) continue;  // 只显示中间的分金
            
            String label = FENJIN_LABELS[posInFive * 2];
            if (label.isEmpty()) continue;
            
            textPaint.setColor(COLOR_TEXT_GRAY);
            float tx = centerX + textR * (float) Math.cos(rad);
            float ty = centerY + textR * (float) Math.sin(rad) + 3 * density;
            canvas.drawText(label, tx, ty, textPaint);
        }

        // 圈边界线
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(1 * density);
        paint.setColor(COLOR_RING_DARK);
        canvas.drawCircle(centerX, centerY, outerR, paint);
        canvas.drawCircle(centerX, centerY, innerR, paint);
    }

    // ========================================================================
    // 5. 六十甲子
    // ========================================================================
    private void drawSexagenary(Canvas canvas) {
        float ringWidth = 16 * density;
        float outerR = radius - 53 * density;
        float innerR = outerR - ringWidth;
        float textR = (outerR + innerR) / 2f;

        // 60个甲子，每6度一个
        float step = 360f / 60f;
        textPaint.setTextSize(9 * density);

        for (int i = 0; i < 60; i++) {
            float angleDeg = i * step;
            float rad = (float) Math.toRadians(angleDeg - 90);
            
            // 甲字红色，庚丙金色，其他灰色
            String label = SEXAGENARY[i];
            if (label.startsWith("甲")) {
                textPaint.setColor(COLOR_TEXT_RED);
            } else if (label.startsWith("庚") || label.startsWith("丙")) {
                textPaint.setColor(COLOR_TEXT_GOLD);
            } else {
                textPaint.setColor(COLOR_TEXT_GRAY);
            }
            
            float tx = centerX + textR * (float) Math.cos(rad);
            float ty = centerY + textR * (float) Math.sin(rad) + 3.5f * density;
            canvas.drawText(label, tx, ty, textPaint);
        }

        // 圈边界线
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(1 * density);
        paint.setColor(COLOR_RING_DARK);
        canvas.drawCircle(centerX, centerY, outerR, paint);
        canvas.drawCircle(centerX, centerY, innerR, paint);
    }

    // ========================================================================
    // 6. 天盘缝针（24山 +7.5°）
    // ========================================================================
    private void drawTianPan(Canvas canvas) {
        float ringWidth = 16 * density;
        float outerR = radius - 71 * density;
        float innerR = outerR - ringWidth;
        float textR = (outerR + innerR) / 2f;

        textPaint.setTextSize(12 * density);

        for (int i = 0; i < 24; i++) {
            // 天盘缝针 = 地盘正针 + 7.5°
            float angleDeg = MOUNTAIN_ANGLES[i] + 7.5f;
            if (angleDeg >= 360) angleDeg -= 360;
            float rad = (float) Math.toRadians(angleDeg - 90);
            
            // 子午卯酉红色
            if (i == 1 || i == 7 || i == 13 || i == 19) {  // 子午卯酉
                textPaint.setColor(COLOR_TEXT_RED);
            } else {
                textPaint.setColor(COLOR_TEXT_BLUE);  // 蓝色表示天盘
            }
            
            float tx = centerX + textR * (float) Math.cos(rad);
            float ty = centerY + textR * (float) Math.sin(rad) + 4.5f * density;
            canvas.drawText(MOUNTAINS_24[i], tx, ty, textPaint);
        }

        // 标注"天"字
        textPaint.setTextSize(8 * density);
        textPaint.setColor(COLOR_TEXT_BLUE);
        float tianRad = (float) Math.toRadians(270 - 90);  // 正北偏下
        float tianX = centerX + (textR - 6 * density) * (float) Math.cos(tianRad);
        float tianY = centerY + (textR - 6 * density) * (float) Math.sin(tianRad) + 3 * density;
        canvas.drawText("天", tianX, tianY, textPaint);

        // 圈边界线
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(1 * density);
        paint.setColor(COLOR_RING_DARK);
        canvas.drawCircle(centerX, centerY, outerR, paint);
        canvas.drawCircle(centerX, centerY, innerR, paint);
    }

    // ========================================================================
    // 7. 人盘中针（24山 -7.5°）
    // ========================================================================
    private void drawRenPan(Canvas canvas) {
        float ringWidth = 16 * density;
        float outerR = radius - 89 * density;
        float innerR = outerR - ringWidth;
        float textR = (outerR + innerR) / 2f;

        textPaint.setTextSize(12 * density);

        for (int i = 0; i < 24; i++) {
            // 人盘中针 = 地盘正针 - 7.5°
            float angleDeg = MOUNTAIN_ANGLES[i] - 7.5f;
            if (angleDeg < 0) angleDeg += 360;
            float rad = (float) Math.toRadians(angleDeg - 90);
            
            // 子午卯酉红色
            if (i == 1 || i == 7 || i == 13 || i == 19) {
                textPaint.setColor(COLOR_TEXT_RED);
            } else {
                textPaint.setColor(COLOR_TEXT_GREEN);  // 绿色表示人盘
            }
            
            float tx = centerX + textR * (float) Math.cos(rad);
            float ty = centerY + textR * (float) Math.sin(rad) + 4.5f * density;
            canvas.drawText(MOUNTAINS_24[i], tx, ty, textPaint);
        }

        // 标注"人"字
        textPaint.setTextSize(8 * density);
        textPaint.setColor(COLOR_TEXT_GREEN);
        float renRad = (float) Math.toRadians(270 - 90);
        float renX = centerX + (textR - 6 * density) * (float) Math.cos(renRad);
        float renY = centerY + (textR - 6 * density) * (float) Math.sin(renRad) + 3 * density;
        canvas.drawText("人", renX, renY, textPaint);

        // 圈边界线
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(1 * density);
        paint.setColor(COLOR_RING_DARK);
        canvas.drawCircle(centerX, centerY, outerR, paint);
        canvas.drawCircle(centerX, centerY, innerR, paint);
    }

    // ========================================================================
    // 8. 地盘正针（24山主圈）
    // ========================================================================
    private void drawDiPan(Canvas canvas) {
        float ringWidth = 20 * density;
        float outerR = radius - 107 * density;
        float innerR = outerR - ringWidth;
        float textR = (outerR + innerR) / 2f;

        textPaint.setTextSize(14 * density);

        for (int i = 0; i < 24; i++) {
            float angleDeg = MOUNTAIN_ANGLES[i];
            float rad = (float) Math.toRadians(angleDeg - 90);
            
            // 子午卯酉（正北、正南、正东、正西）红色加大
            if (i == 1 || i == 7 || i == 13 || i == 19) {
                textPaint.setColor(COLOR_TEXT_RED);
                textPaint.setTextSize(16 * density);
            } else {
                textPaint.setColor(COLOR_TEXT_GOLD);
                textPaint.setTextSize(14 * density);
            }
            
            float tx = centerX + textR * (float) Math.cos(rad);
            float ty = centerY + textR * (float) Math.sin(rad) + 5 * density;
            canvas.drawText(MOUNTAINS_24[i], tx, ty, textPaint);
        }

        // 标注"地"字
        textPaint.setTextSize(9 * density);
        textPaint.setColor(COLOR_TEXT_GOLD);
        float diRad = (float) Math.toRadians(270 - 90);
        float diX = centerX + (textR - 7 * density) * (float) Math.cos(diRad);
        float diY = centerY + (textR - 7 * density) * (float) Math.sin(diRad) + 3.5f * density;
        canvas.drawText("地", diX, diY, textPaint);

        // 圈边界线
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(1.5f * density);
        paint.setColor(COLOR_RING_DARK);
        canvas.drawCircle(centerX, centerY, outerR, paint);
        canvas.drawCircle(centerX, centerY, innerR, paint);
    }

    // ========================================================================
    // 9. 六十四卦
    // ========================================================================
    private void drawHexagram(Canvas canvas) {
        float ringWidth = 18 * density;
        float outerR = radius - 129 * density;
        float innerR = outerR - ringWidth;
        float textR = (outerR + innerR) / 2f;

        // 64卦，每卦5.625度
        float step = 360f / 64f;
        textPaint.setTextSize(9 * density);

        for (int i = 0; i < 64; i++) {
            float angleDeg = i * step;
            float rad = (float) Math.toRadians(angleDeg - 90);
            
            // 八纯卦红色
            boolean isPure = (i == 0 || i == 7 || i == 17 || i == 24 || 
                             i == 33 || i == 42 || i == 51 || i == 58);
            if (isPure) {
                textPaint.setColor(COLOR_TEXT_RED);
            } else {
                textPaint.setColor(COLOR_TEXT_GOLD);
            }
            
            float tx = centerX + textR * (float) Math.cos(rad);
            float ty = centerY + textR * (float) Math.sin(rad) + 4 * density;
            canvas.drawText(HEXAGRAM_64[i], tx, ty, textPaint);
        }

        // 圈边界线
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(1 * density);
        paint.setColor(COLOR_RING_DARK);
        canvas.drawCircle(centerX, centerY, outerR, paint);
        canvas.drawCircle(centerX, centerY, innerR, paint);
    }

    // ========================================================================
    // 10. 后天八卦
    // ========================================================================
    private void drawBagua(Canvas canvas) {
        float baguaR = radius - 152 * density;
        float dotR = 13 * density;

        for (int i = 0; i < BAGUA.length; i++) {
            float rad = (float) Math.toRadians(BAGUA_ANGLES[i] - 90);
            float bx = centerX + baguaR * (float) Math.cos(rad);
            float by = centerY + baguaR * (float) Math.sin(rad);

            // 八卦背景
            sectorPaint.setColor(COLOR_BAGUA_BG);
            canvas.drawCircle(bx, by, dotR, sectorPaint);

            // 八卦边框
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(1 * density);
            paint.setColor(COLOR_RING_DARK);
            canvas.drawCircle(bx, by, dotR, paint);

            // 八卦文字
            if (i == 0) textPaint.setColor(Color.BLACK);       // 坎=黑
            else if (i == 4) textPaint.setColor(COLOR_TEXT_RED); // 离=红
            else textPaint.setColor(COLOR_TEXT_GOLD);
            
            textPaint.setTextSize(15 * density);
            canvas.drawText(BAGUA[i], bx, by + 5 * density, textPaint);
        }

        // 八卦外圈
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(1 * density);
        paint.setColor(COLOR_RING_DARK);
        canvas.drawCircle(centerX, centerY, baguaR + dotR + 3 * density, paint);
        canvas.drawCircle(centerX, centerY, baguaR - dotR - 3 * density, paint);
    }

    // ========================================================================
    // 11. 360度刻度
    // ========================================================================
    private void drawDegreeScale(Canvas canvas) {
        float outerR = radius - 172 * density;
        float innerR;

        for (int i = 0; i < 360; i++) {
            float angle = (float) Math.toRadians(i - 90);

            if (i % 15 == 0) {
                // 15度刻度 - 长线
                tickPaint.setStrokeWidth(2.5f * density);
                tickPaint.setColor(COLOR_TEXT_GOLD);
                innerR = outerR - 14 * density;
            } else if (i % 5 == 0) {
                // 5度刻度 - 中线
                tickPaint.setStrokeWidth(1.5f * density);
                tickPaint.setColor(COLOR_TEXT_GRAY);
                innerR = outerR - 10 * density;
            } else {
                // 1度刻度 - 短线
                tickPaint.setStrokeWidth(0.8f * density);
                tickPaint.setColor(COLOR_RING_DARK);
                innerR = outerR - 6 * density;
            }

            float sx = centerX + outerR * (float) Math.cos(angle);
            float sy = centerY + outerR * (float) Math.sin(angle);
            float ex = centerX + innerR * (float) Math.cos(angle);
            float ey = centerY + innerR * (float) Math.sin(angle);
            canvas.drawLine(sx, sy, ex, ey, tickPaint);
        }

        // 30度数字
        textPaint.setTextSize(9 * density);
        textPaint.setColor(COLOR_TEXT_GRAY);
        float textR = outerR - 18 * density;
        for (int i = 0; i < 360; i += 30) {
            if (i == 0) continue;  // 正北用"北"字表示
            float rad = (float) Math.toRadians(i - 90);
            float tx = centerX + textR * (float) Math.cos(rad);
            float ty = centerY + textR * (float) Math.sin(rad) + 3.5f * density;
            canvas.drawText(String.valueOf(i), tx, ty, textPaint);
        }

        // 四正方向标注（北南东西）
        textPaint.setTextSize(10 * density);
        String[] cardinal = {"北", "东", "南", "西"};
        int[] cardinalAngles = {0, 90, 180, 270};
        float cardR = outerR - 22 * density;
        for (int i = 0; i < 4; i++) {
            float rad = (float) Math.toRadians(cardinalAngles[i] - 90);
            textPaint.setColor(i == 2 ? COLOR_TEXT_RED : COLOR_TEXT_GOLD);
            float tx = centerX + cardR * (float) Math.cos(rad);
            float ty = centerY + cardR * (float) Math.sin(rad) + 4 * density;
            canvas.drawText(cardinal[i], tx, ty, textPaint);
        }
    }

    // ========================================================================
    // 12. 天池（中心太极圈）
    // ========================================================================
    private void drawTianchi(Canvas canvas) {
        float tianchiR = 22 * density;

        // 天池外框（金色）
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(2 * density);
        paint.setColor(COLOR_RING_GOLD);
        canvas.drawCircle(centerX, centerY, tianchiR + 4 * density, paint);

        // 天池内背景
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(COLOR_CENTER_BG);
        canvas.drawCircle(centerX, centerY, tianchiR, paint);

        // 天池底部红线（正南北方向）
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(1 * density);
        paint.setColor(COLOR_RED_LINE);
        
        float lineR = tianchiR - 2 * density;
        // 上端（指南端）
        canvas.drawLine(centerX, centerY - lineR, centerX, centerY - 4 * density, paint);
        // 下端（指北端）
        canvas.drawLine(centerX, centerY + 4 * density, centerX, centerY + lineR, paint);

        // 红线两端的两个小红点（指北端）
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(COLOR_RED_LINE);
        float dotR = 1.5f * density;
        canvas.drawCircle(centerX, centerY + 6 * density, dotR, paint);
        canvas.drawCircle(centerX, centerY + 9 * density, dotR, paint);

        // 中心小圆点
        paint.setColor(COLOR_RING_GOLD);
        canvas.drawCircle(centerX, centerY, 2.5f * density, paint);
    }

    // ========================================================================
    // 13. 指针（磁针）
    // ========================================================================
    private void drawNeedle(Canvas canvas) {
        float needleLen = 55 * density;  // 指针长度
        float needleBase = 14 * density;  // 指针底座宽度
        float tipWidth = 2.5f * density;  // 针尖宽度

        canvas.save();
        // 指针随方位角旋转
        canvas.rotate(azimuth, centerX, centerY);

        // 红色指针（南端 - 尖头）
        needlePaint.setColor(COLOR_NEEDLE_SOUTH);
        needlePaint.setStyle(Paint.Style.FILL);
        
        Path southPath = new Path();
        southPath.moveTo(centerX, centerY - needleLen);  // 尖端
        southPath.lineTo(centerX - tipWidth, centerY);   // 左宽
        southPath.lineTo(centerX + tipWidth, centerY);  // 右宽
        southPath.close();
        canvas.drawPath(southPath, needlePaint);

        // 黑色指针（北端 - 有角）
        needlePaint.setColor(COLOR_NEEDLE_NORTH);
        
        Path northPath = new Path();
        northPath.moveTo(centerX, centerY + needleLen);  // 尖端
        northPath.lineTo(centerX - needleBase, centerY);  // 左角
        northPath.lineTo(centerX + needleBase, centerY); // 右角
        northPath.close();
        canvas.drawPath(northPath, needlePaint);

        // 指针中心覆盖（天池顶部）
        needlePaint.setColor(COLOR_CENTER_BG);
        canvas.drawCircle(centerX, centerY, 10 * density, needlePaint);
        
        // 中心金点
        needlePaint.setColor(COLOR_RING_GOLD);
        canvas.drawCircle(centerX, centerY, 3 * density, needlePaint);

        canvas.restore();
    }

    // ========================================================================
    // 14. 信息显示
    // ========================================================================
    private void drawInfo(Canvas canvas) {
        // 顶部信息栏
        textPaint.setTextAlign(Paint.Align.LEFT);
        textPaint.setTextSize(12 * density);
        textPaint.setColor(COLOR_TEXT_WHITE);

        int mountainIdx = getCurrentMountain(azimuth);
        String mountainName = MOUNTAINS_24[mountainIdx];
        String directionName = getDirectionName(azimuth);
        String xiuName = getCurrentXiu();

        String info = String.format("方位: %.1f°  %s山  %s  %s",
                azimuth, mountainName, directionName, xiuName);
        canvas.drawText(info, 12 * density, 24 * density, textPaint);

        // 底部三盘信息
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTextSize(9 * density);
        textPaint.setColor(COLOR_TEXT_GRAY);

        int tianIdx = getTianPanMountain(azimuth);
        int renIdx = getRenPanMountain(azimuth);

        String bottomInfo = String.format("天盘:%s  人盘:%s  地盘:%s",
                MOUNTAINS_24[tianIdx], MOUNTAINS_24[renIdx], mountainName);
        canvas.drawText(bottomInfo, centerX, getHeight() - 10 * density, textPaint);

        textPaint.setTextAlign(Paint.Align.LEFT);
    }

    // ============ 辅助方法 ============

    private int getCurrentMountain(float angle) {
        angle = angle % 360;
        if (angle < 0) angle += 360;
        return (int) Math.round(angle / 15f) % 24;
    }

    private int getTianPanMountain(float angle) {
        float a = angle - 7.5f;
        a = a % 360;
        if (a < 0) a += 360;
        return (int) Math.round(a / 15f) % 24;
    }

    private int getRenPanMountain(float angle) {
        float a = angle + 7.5f;
        a = a % 360;
        if (a < 0) a += 360;
        return (int) Math.round(a / 15f) % 24;
    }

    private String getCurrentXiu(float angle) {
        angle = angle % 360;
        if (angle < 0) angle += 360;
        float step = 360f / 28f;
        int closest = 0;
        float minDiff = 360;
        for (int i = 0; i < 28; i++) {
            float diff = Math.abs(angle - XIU_ANGLES[i]);
            if (diff > 180) diff = 360 - diff;
            if (diff < minDiff) {
                minDiff = diff;
                closest = i;
            }
        }
        return XIU_28[closest];
    }

    private String getDirectionName(float degrees) {
        String[] dirs = {"北", "东北", "东", "东南", "南", "西南", "西", "西北"};
        int[] ranges = {0, 45, 90, 135, 180, 225, 270, 315};
        String closest = dirs[0];
        float minDiff = 360;
        for (int i = 0; i < ranges.length; i++) {
            float diff = Math.abs(degrees - ranges[i]);
            if (diff > 180) diff = 360 - diff;
            if (diff < minDiff) {
                minDiff = diff;
                closest = dirs[i];
            }
        }
        return closest;
    }
}
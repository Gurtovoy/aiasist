package com.example.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlin.math.sin

enum class VTuberExpression {
    NORMAL,
    ANGRY,
    SHY_BLUSH,
    HAPPY
}

@Composable
fun VTuberCharacter(
    isSpeaking: Boolean,
    isThinking: Boolean,
    isListening: Boolean,
    expression: VTuberExpression,
    modifier: Modifier = Modifier,
    onTap: () -> Unit = {}
) {
    // Infinite transition for breathing, hair sway, and ear twitches
    val infiniteTransition = rememberInfiniteTransition(label = "VTuberAnimation")
    
    // Breathing cycle: moves head & shoulders slightly up and down
    val breathingOffset by infiniteTransition.animateFloat(
        initialValue = -5f,
        targetValue = 5f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Breathing"
    )

    // Hair sway: horizontal oscillation for bangs and side hair
    val hairSway by infiniteTransition.animateFloat(
        initialValue = -4f,
        targetValue = 4f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "HairSway"
    )

    // Eyeblinking cycle: eyes scaleY drops to 0 periodically
    val eyeBlink by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 3200
                1f at 0
                1f at 2900 // stay open
                0f at 3000 // close
                1f at 3100 // reopen
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "EyeBlink"
    )

    // Lip-sync mouth animation when speaking
    val mouthScale by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(150, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "LipSync"
    )

    // Listening ear wiggle animation
    val earWiggleState by infiniteTransition.animateFloat(
        initialValue = -3f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(
            animation = tween(450, easing = EaseInOutQuad),
            repeatMode = RepeatMode.Reverse
        ),
        label = "EarWiggle"
    )

    // Interactive floating hearts/stars generator when happy
    val sparkOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -12f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = EaseInOutQuad),
            repeatMode = RepeatMode.Reverse
        ),
        label = "SparkOffset"
    )

    // Smooth transition for expression changes (e.g. blush opacity)
    val blushIntensity by animateFloatAsState(
        targetValue = when (expression) {
            VTuberExpression.SHY_BLUSH -> 0.85f
            VTuberExpression.ANGRY -> 0.45f
            VTuberExpression.HAPPY -> 0.3f
            else -> 0.12f
        },
        animationSpec = tween(400),
        label = "BlushIntensity"
    )

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onTap
            )
    ) {
        val width = size.width
        val height = size.height
        
        // Define coordinates relative to Canvas size
        val centerX = width / 2f
        val centerY = height * 0.48f // central point of the face
        
        // Dynamic offset based on breathing
        val headY = centerY + breathingOffset * 0.4f

        // --- BACKGROUND GLOW ---
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFFFF80AB).copy(alpha = 0.22f), // Soft Maid Pink Glow
                    Color(0xFF7C4DFF).copy(alpha = 0.04f),
                    Color.Transparent
                ),
                center = Offset(centerX, centerY),
                radius = width * 0.65f
            )
        )

        // --- SECTION 1: KAWAII MAID DRESS (SHOULDERS & APRON) ---
        // Dark uniform base shape
        val dressPath = Path().apply {
            moveTo(centerX - width * 0.28f, height)
            quadraticTo(
                centerX - width * 0.22f, centerY + 140f + breathingOffset * 0.5f,
                centerX - width * 0.09f, centerY + 105f + breathingOffset
            )
            lineTo(centerX + width * 0.09f, centerY + 105f + breathingOffset)
            quadraticTo(
                centerX + width * 0.22f, centerY + 140f + breathingOffset * 0.5f,
                centerX + width * 0.28f, height
            )
            close()
        }
        drawPath(path = dressPath, color = Color(0xFF1E1D24)) // Deep coal maid dress color

        // Pristine White Apron Straps (Лямки фартучка на плечах)
        val leftStrap = Path().apply {
            moveTo(centerX - 42f, centerY + 105f + breathingOffset)
            quadraticTo(centerX - 70f, centerY + 145f + breathingOffset, centerX - 80f, height)
            lineTo(centerX - 52f, height)
            quadraticTo(centerX - 42f, centerY + 135f + breathingOffset, centerX - 24f, centerY + 105f + breathingOffset)
            close()
        }
        drawPath(path = leftStrap, color = Color.White)

        val rightStrap = Path().apply {
            moveTo(centerX + 42f, centerY + 105f + breathingOffset)
            quadraticTo(centerX + 70f, centerY + 145f + breathingOffset, centerX + 80f, height)
            lineTo(centerX + 52f, height)
            quadraticTo(centerX + 42f, centerY + 135f + breathingOffset, centerX + 24f, centerY + 105f + breathingOffset)
            close()
        }
        drawPath(path = rightStrap, color = Color.White)

        // Maid apron chest bib (Грудка фартука)
        val bibPath = Path().apply {
            moveTo(centerX - 24f, centerY + 115f + breathingOffset)
            lineTo(centerX - 35f, height)
            lineTo(centerX + 35f, height)
            lineTo(centerX + 24f, centerY + 115f + breathingOffset)
            close()
        }
        drawPath(path = bibPath, color = Color(0xFFFCE4EC)) // Subtle soft pinkish white

        // Pristine White Maid Collar Trim (Воротничок горничной)
        val collarPath = Path().apply {
            moveTo(centerX - 32f, centerY + 102f + breathingOffset)
            quadraticTo(centerX, centerY + 120f + breathingOffset, centerX + 32f, centerY + 102f + breathingOffset)
            lineTo(centerX + 22f, centerY + 94f + breathingOffset)
            quadraticTo(centerX, centerY + 105f + breathingOffset, centerX - 22f, centerY + 94f + breathingOffset)
            close()
        }
        drawPath(path = collarPath, color = Color(0xFFF3E5F5)) // Lace white color

        // --- SECTION 2: NECK & SKIN ZONE ---
        val neckPath = Path().apply {
            moveTo(centerX - 22f, centerY + 50f + breathingOffset * 0.4f)
            lineTo(centerX - 22f, centerY + 108f + breathingOffset)
            lineTo(centerX + 22f, centerY + 108f + breathingOffset)
            lineTo(centerX + 22f, centerY + 50f + breathingOffset * 0.4f)
            close()
        }
        drawPath(path = neckPath, color = Color(0xFFFFD1A9)) // Soft skin shade
        
        // Neck shadow
        drawPath(
            path = Path().apply {
                moveTo(centerX - 22f, centerY + 50f + breathingOffset * 0.4f)
                lineTo(centerX, centerY + 78f + breathingOffset * 0.4f)
                lineTo(centerX + 22f, centerY + 50f + breathingOffset * 0.4f)
                close()
            },
            color = Color(0xFFF48FB1).copy(alpha = 0.25f) // Rose shadow on neck
        )

        // --- SECTION 3: BRIGHT CHEST RIBBON BOW (Бантик на фартуке) ---
        val bowY = centerY + 116f + breathingOffset
        // Left loop
        val leftBowLoop = Path().apply {
            moveTo(centerX, bowY)
            cubicTo(centerX - 30f, bowY - 14f, centerX - 30f, bowY + 14f, centerX, bowY)
        }
        drawPath(path = leftBowLoop, color = Color(0xFFFF4081)) // Neon pink ribbon

        // Right loop
        val rightBowLoop = Path().apply {
            moveTo(centerX, bowY)
            cubicTo(centerX + 30f, bowY - 14f, centerX + 30f, bowY + 14f, centerX, bowY)
        }
        drawPath(path = rightBowLoop, color = Color(0xFFFF4081))

        // Bow tails
        drawPath(
            path = Path().apply {
                moveTo(centerX - 3f, bowY)
                lineTo(centerX - 22f, bowY + 26f)
                lineTo(centerX - 10f, bowY + 28f)
                close()
            },
            color = Color(0xFFE91E63)
        )
        drawPath(
            path = Path().apply {
                moveTo(centerX + 3f, bowY)
                lineTo(centerX + 22f, bowY + 26f)
                lineTo(centerX + 10f, bowY + 28f)
                close()
            },
            color = Color(0xFFE91E63)
        )

        // Gold gemstone or brooch in the bow's middle
        drawCircle(color = Color(0xFFFFD54F), radius = 6.5f, center = Offset(centerX, bowY))
        drawCircle(color = Color.White, radius = 2f, center = Offset(centerX - 1.5f, bowY - 1.5f))

        // --- SECTION 4: ANIME CAT-MAID EARS (Пушистые ушки с розовым ворсом) ---
        val activeEarWiggle = if (isListening || isThinking) earWiggleState * 2.5f else 0f
        
        // Left Cat Ear
        val leftEarPath = Path().apply {
            moveTo(centerX - 92f, headY - 50f)
            quadraticTo(
                centerX - 122f + activeEarWiggle, headY - 135f,
                centerX - 52f, headY - 85f
            )
            close()
        }
        drawPath(path = leftEarPath, color = Color(0xFF2B2930)) // Charcoal black matching headband bows
        
        // Left Inner Ear (Pink)
        val leftInnerEarPath = Path().apply {
            moveTo(centerX - 87f, headY - 55f)
            quadraticTo(
                centerX - 110f + activeEarWiggle, headY - 123f,
                centerX - 58f, headY - 82f
            )
            close()
        }
        drawPath(path = leftInnerEarPath, color = Color(0xFFFF80AB)) // Vivid kawaii pink inside

        // Right Cat Ear
        val rightEarPath = Path().apply {
            moveTo(centerX + 92f, headY - 50f)
            quadraticTo(
                centerX + 122f - activeEarWiggle, headY - 135f,
                centerX + 52f, headY - 85f
            )
            close()
        }
        drawPath(path = rightEarPath, color = Color(0xFF2B2930))
        
        // Right Inner Ear (Pink)
        val rightInnerEarPath = Path().apply {
            moveTo(centerX + 87f, headY - 55f)
            quadraticTo(
                centerX + 110f - activeEarWiggle, headY - 123f,
                centerX + 58f, headY - 82f
            )
            close()
        }
        drawPath(path = rightInnerEarPath, color = Color(0xFFFF80AB))

        // --- SECTION 5: ANIME FACE BASE ---
        val faceRadiusWidth = 88f
        val faceRadiusHeight = 94f

        drawOval(
            color = Color(0xFFFFECCC), // Gorgeous creamy anime skin tone
            topLeft = Offset(centerX - faceRadiusWidth, headY - faceRadiusHeight),
            size = Size(faceRadiusWidth * 2, faceRadiusHeight * 2)
        )

        // --- SECTION 6: ROSY BLUSH & EMOTIONAL EFFECTS ---
        if (blushIntensity > 0f) {
            val cheekY = headY + 12f
            val blushColor = if (expression == VTuberExpression.ANGRY) Color(0xFFFF4D4D) else Color(0xFFFF6090)
            
            // Left cheek blush
            drawOval(
                color = blushColor.copy(alpha = blushIntensity),
                topLeft = Offset(centerX - 62f, cheekY),
                size = Size(32f, 14f)
            )
            // Right cheek blush
            drawOval(
                color = blushColor.copy(alpha = blushIntensity),
                topLeft = Offset(centerX + 30f, cheekY),
                size = Size(32f, 14f)
            )

            // Shy embarrass diagonal lines
            if (expression == VTuberExpression.SHY_BLUSH) {
                for (i in 0 until 3) {
                    val xL = (centerX - 52f) + i * 8f
                    drawLine(
                        color = Color.White.copy(alpha = 0.65f),
                        start = Offset(xL, cheekY - 3f),
                        end = Offset(xL - 4f, cheekY + 10f),
                        strokeWidth = 2f
                    )
                    val xR = (centerX + 38f) + i * 8f
                    drawLine(
                        color = Color.White.copy(alpha = 0.65f),
                        start = Offset(xR, cheekY - 3f),
                        end = Offset(xR - 4f, cheekY + 10f),
                        strokeWidth = 2f
                    )
                }
            }
        }

        // --- SECTION 7: ANIME EYES & EYEBROWS ---
        val eyeY = headY - 12f
        val eyeWidth = 28f
        val eyeHeight = 35f
        val leftEyeX = centerX - 46f
        val rightEyeX = centerX + 18f

        // Eyebrow tilts reflecting emotions
        val eyebrowTilt = when (expression) {
            VTuberExpression.ANGRY -> 9f
            VTuberExpression.SHY_BLUSH -> -3f
            VTuberExpression.HAPPY -> -5f
            else -> 0f
        }

        // Left Eyebrow
        drawPath(
            path = Path().apply {
                moveTo(leftEyeX - 4f, eyeY - 22f + eyebrowTilt)
                quadraticTo(
                    leftEyeX + 10f, eyeY - 29f + (eyebrowTilt * 0.5f),
                    leftEyeX + eyeWidth + 4f, eyeY - 20f - eyebrowTilt
                )
            },
            color = Color(0xFFF06292), // Styled magenta-indigo eyebrow matching hair
            style = Stroke(width = 4.5f, cap = StrokeCap.Round)
        )

        // Right Eyebrow
        drawPath(
            path = Path().apply {
                moveTo(rightEyeX - 4f, eyeY - 20f - eyebrowTilt)
                quadraticTo(
                    rightEyeX + eyeWidth - 10f, eyeY - 29f + (eyebrowTilt * 0.5f),
                    rightEyeX + eyeWidth + 4f, eyeY - 22f + eyebrowTilt
                )
            },
            color = Color(0xFFF06292),
            style = Stroke(width = 4.5f, cap = StrokeCap.Round)
        )

        // LEFT EYE
        when (expression) {
            VTuberExpression.HAPPY -> {
                // Happy curved eyes "^"
                val eyeArc = Path().apply {
                    moveTo(leftEyeX - 3f, eyeY + 4f)
                    quadraticTo(leftEyeX + eyeWidth / 2f, eyeY - 14f, leftEyeX + eyeWidth + 3f, eyeY + 4f)
                }
                drawPath(path = eyeArc, color = Color(0xFFE91E63), style = Stroke(width = 6f, cap = StrokeCap.Round))
                
                // Overlay spark heart/reflection next to eyes
                drawCircle(color = Color(0xFFFF80AB), radius = 3.5f + sparkOffset * -0.2f, center = Offset(leftEyeX - 12f, eyeY - 15f + sparkOffset))
            }
            else -> {
                val currentEyeHeight = eyeHeight * eyeBlink
                if (currentEyeHeight > 3f) {
                    // Sclera base
                    drawOval(
                        color = Color.White,
                        topLeft = Offset(leftEyeX, eyeY - currentEyeHeight / 2),
                        size = Size(eyeWidth, currentEyeHeight)
                    )
                    // Beautiful Pinkish Violet Maid Iris
                    drawOval(
                        brush = Brush.verticalGradient(listOf(Color(0xFFFF4081), Color(0xFF7C4DFF))),
                        topLeft = Offset(leftEyeX + 2.5f, eyeY - currentEyeHeight * 0.42f),
                        size = Size(eyeWidth - 5f, currentEyeHeight * 0.84f)
                    )
                    // High-quality black pupil
                    drawCircle(
                        color = Color(0xFF311B92),
                        center = Offset(leftEyeX + eyeWidth / 2f, eyeY),
                        radius = (eyeWidth - 12f) / 2f * eyeBlink
                    )
                    // Shimmering reflection pupil stars
                    if (expression == VTuberExpression.SHY_BLUSH) {
                        // Tiny cute heart inside pupil
                        drawCircle(color = Color(0xFFFF80AB), radius = 2.5f, center = Offset(leftEyeX + eyeWidth / 2f, eyeY + 2f))
                    }
                    // Crisp main capture highlights
                    drawCircle(
                        color = Color.White,
                        center = Offset(leftEyeX + 8f, eyeY - currentEyeHeight * 0.22f),
                        radius = 4.2f * eyeBlink
                    )
                    drawCircle(
                        color = Color.White,
                        center = Offset(leftEyeX + eyeWidth - 8f, eyeY + currentEyeHeight * 0.22f),
                        radius = 2.5f * eyeBlink
                    )
                    // Thick cute manga lashes line
                    drawLine(
                        color = Color(0xFF2B2930),
                        start = Offset(leftEyeX - 4f, eyeY - currentEyeHeight / 2),
                        end = Offset(leftEyeX + eyeWidth + 4f, eyeY - currentEyeHeight / 2),
                        strokeWidth = 5.5f
                    )
                } else {
                    // Closed eye lash line
                    drawLine(
                        color = Color(0xFF2B2930),
                        start = Offset(leftEyeX - 4f, eyeY),
                        end = Offset(leftEyeX + eyeWidth + 4f, eyeY),
                        strokeWidth = 5.5f
                    )
                }
            }
        }

        // RIGHT EYE
        when (expression) {
            VTuberExpression.HAPPY -> {
                val eyeArc = Path().apply {
                    moveTo(rightEyeX - 3f, eyeY + 4f)
                    quadraticTo(rightEyeX + eyeWidth / 2f, eyeY - 14f, rightEyeX + eyeWidth + 3f, eyeY + 4f)
                }
                drawPath(path = eyeArc, color = Color(0xFFE91E63), style = Stroke(width = 6f, cap = StrokeCap.Round))
                
                // Spark shadow
                drawCircle(color = Color(0xFFFF80AB), radius = 3.5f + sparkOffset * -0.2f, center = Offset(rightEyeX + eyeWidth + 12f, eyeY - 15f + sparkOffset))
            }
            else -> {
                val currentEyeHeight = eyeHeight * eyeBlink
                if (currentEyeHeight > 3f) {
                    drawOval(
                        color = Color.White,
                        topLeft = Offset(rightEyeX, eyeY - currentEyeHeight / 2),
                        size = Size(eyeWidth, currentEyeHeight)
                    )
                    drawOval(
                        brush = Brush.verticalGradient(listOf(Color(0xFFFF4081), Color(0xFF7C4DFF))),
                        topLeft = Offset(rightEyeX + 2.5f, eyeY - currentEyeHeight * 0.42f),
                        size = Size(eyeWidth - 5f, currentEyeHeight * 0.84f)
                    )
                    drawCircle(
                        color = Color(0xFF311B92),
                        center = Offset(rightEyeX + eyeWidth / 2f, eyeY),
                        radius = (eyeWidth - 12f) / 2f * eyeBlink
                    )
                    if (expression == VTuberExpression.SHY_BLUSH) {
                        drawCircle(color = Color(0xFFFF80AB), radius = 2.5f, center = Offset(rightEyeX + eyeWidth / 2f, eyeY + 2f))
                    }
                    drawCircle(
                        color = Color.White,
                        center = Offset(rightEyeX + 8f, eyeY - currentEyeHeight * 0.22f),
                        radius = 4.2f * eyeBlink
                    )
                    drawCircle(
                        color = Color.White,
                        center = Offset(rightEyeX + eyeWidth - 8f, eyeY + currentEyeHeight * 0.22f),
                        radius = 2.5f * eyeBlink
                    )
                    drawLine(
                        color = Color(0xFF2B2930),
                        start = Offset(rightEyeX - 4f, eyeY - currentEyeHeight / 2),
                        end = Offset(rightEyeX + eyeWidth + 4f, eyeY - currentEyeHeight / 2),
                        strokeWidth = 5.5f
                    )
                } else {
                    drawLine(
                        color = Color(0xFF2B2930),
                        start = Offset(rightEyeX - 4f, eyeY),
                        end = Offset(rightEyeX + eyeWidth + 4f, eyeY),
                        strokeWidth = 5.5f
                    )
                }
            }
        }

        // --- SECTION 8: MOUTH (LIP SYNC / CHAT EXPRESSION) ---
        val mouthY = headY + 31f
        if (isSpeaking) {
            val h = 10f + (22f * mouthScale)
            drawOval(
                color = Color(0xFFC2185B), // Deep inner cavity
                topLeft = Offset(centerX - 13f, mouthY - h / 2f),
                size = Size(26f, h)
            )
            // Tiny tongue
            drawOval(
                color = Color(0xFFFF8A80),
                topLeft = Offset(centerX - 9f, mouthY + h * 0.12f),
                size = Size(18f, h * 0.38f)
            )
            // Lip stroke outline
            drawOval(
                color = Color(0xFFE91E63),
                topLeft = Offset(centerX - 13f, mouthY - h / 2f),
                size = Size(26f, h),
                style = Stroke(width = 3.5f)
            )
        } else {
            when (expression) {
                VTuberExpression.HAPPY -> {
                    // Big cute cat smile
                    val happyMouth = Path().apply {
                        moveTo(centerX - 13f, mouthY - 2f)
                        quadraticTo(centerX, mouthY + 11f, centerX + 13f, mouthY - 2f)
                    }
                    drawPath(path = happyMouth, color = Color(0xFFD81B60), style = Stroke(width = 4.5f, cap = StrokeCap.Round))
                }
                VTuberExpression.ANGRY -> {
                    // Unhappy classic pout curve
                    val angryMouth = Path().apply {
                        moveTo(centerX - 11f, mouthY + 5f)
                        quadraticTo(centerX, mouthY - 2f, centerX + 11f, mouthY + 5f)
                    }
                    drawPath(path = angryMouth, color = Color(0xFF3E2723), style = Stroke(width = 4.5f, cap = StrokeCap.Round))
                }
                else -> {
                    // Kawaii cat mouth shape "w"
                    val catMouth = Path().apply {
                        moveTo(centerX - 11f, mouthY)
                        quadraticTo(centerX - 5.5f, mouthY + 5.5f, centerX, mouthY + 1f)
                        quadraticTo(centerX + 5.5f, mouthY + 5.5f, centerX + 11f, mouthY)
                    }
                    drawPath(path = catMouth, color = Color(0xFFE91E63), style = Stroke(width = 4.2f, cap = StrokeCap.Round))
                }
            }
        }

        // Cute blush button nose
        drawCircle(
            color = Color(0xFFF48FB1).copy(alpha = 0.8f),
            center = Offset(centerX, headY + 10f),
            radius = 2.5f
        )

        // --- SECTION 9: LUSCIOUS MAID HAIR (SOFT COTTON-CANDY PINK GRADIENT) ---
        val hairPrimaryColor = Color(0xFFFFC0CB) // Pink base
        val hairHighlightColor = Color(0xFFFFE4E1) // Soft rosewhite highlight
        val hairShadowColor = Color(0xFFEC407A) // Vibrant pink shading

        // Back Hair Base
        drawCircle(
            brush = Brush.verticalGradient(listOf(hairShadowColor, hairPrimaryColor)),
            center = Offset(centerX, headY - 14f),
            radius = 100f
        )

        // Left Headband Frill Node (Ribbons at the side braid joints)
        drawCircle(color = Color.White, radius = 9f, center = Offset(centerX - 82f + hairSway * 0.4f, headY - 48f))
        
        // Left Hair bunch/Twin tail waving
        val leftTailPath = Path().apply {
            moveTo(centerX - 80f + hairSway, headY - 45f)
            quadraticTo(centerX - 115f + hairSway * 1.5f, headY + 38f, centerX - 92f + hairSway * 1.2f, headY + 84f)
            quadraticTo(centerX - 98f + hairSway * 1.5f, headY + 32f, centerX - 70f, headY - 18f)
            close()
        }
        drawPath(path = leftTailPath, brush = Brush.verticalGradient(listOf(hairShadowColor, Color(0xFFFFE0B2))))

        // Right Headband Frill Node
        drawCircle(color = Color.White, radius = 9f, center = Offset(centerX + 82f - hairSway * 0.4f, headY - 48f))

        // Right Hair bunch/Twin tail waving
        val rightTailPath = Path().apply {
            moveTo(centerX + 80f - hairSway, headY - 45f)
            quadraticTo(centerX + 115f - hairSway * 1.5f, headY + 38f, centerX + 92f - hairSway * 1.2f, headY + 84f)
            quadraticTo(centerX + 98f - hairSway * 1.5f, headY + 32f, centerX + 70f, headY - 18f)
            close()
        }
        drawPath(path = rightTailPath, brush = Brush.verticalGradient(listOf(hairShadowColor, Color(0xFFFFE0B2))))

        // Front Hair Bangs covering forehead with gorgeous locks
        val bangsPath = Path().apply {
            // Left lock
            moveTo(centerX - 90f, headY - 35f)
            quadraticTo(centerX - 65f + hairSway * 0.8f, headY + 11f, centerX - 42f, headY - 10f)
            // Center drop lock
            quadraticTo(centerX + hairSway * 0.4f, headY + 28f, centerX + 10f, headY - 22f)
            // Right lock
            quadraticTo(centerX + 50f + hairSway * 0.8f, headY + 16f, centerX + 88f, headY - 35f)
            lineTo(centerX + 60f, headY - 55f)
            lineTo(centerX - 60f, headY - 55f)
            close()
        }
        drawPath(path = bangsPath, brush = Brush.verticalGradient(listOf(hairPrimaryColor, hairShadowColor)))

        // Elegant thick side bangs hugging the face cheeks
        val leftFrameBang = Path().apply {
            moveTo(centerX - 76f, headY - 18f)
            quadraticTo(centerX - 88f + hairSway, headY + 30f, centerX - 76f + hairSway, headY + 68f)
            quadraticTo(centerX - 82f, headY + 28f, centerX - 66f, headY - 5f)
            close()
        }
        drawPath(path = leftFrameBang, color = hairShadowColor)

        val rightFrameBang = Path().apply {
            moveTo(centerX + 76f, headY - 18f)
            quadraticTo(centerX + 88f - hairSway, headY + 30f, centerX + 76f - hairSway, headY + 68f)
            quadraticTo(centerX + 82f, headY + 28f, centerX + 66f, headY - 5f)
            close()
        }
        drawPath(path = rightFrameBang, color = hairShadowColor)

        // --- SECTION 10: GLORIOUS FRILLED MAID HEADBAND (Белоснежный кружевной ободок) ---
        // Thick white base arc
        val headbandBase = Path().apply {
            moveTo(centerX - 68f, headY - 60f)
            quadraticTo(centerX, headY - 96f, centerX + 68f, headY - 60f)
        }
        drawPath(path = headbandBase, color = Color.White, style = Stroke(width = 16f, cap = StrokeCap.Round))

        // Lace circles for frill scallops along the top edge of the headband base
        for (i in 0..7) {
            val progress = i / 7f
            val radAngle = progress * Math.PI.toFloat()
            // Circle positions along the top curve
            val x = centerX - 64f + progress * 128f
            val y = headY - 62f - (34f * sin(radAngle))
            
            // Draw overlapping frills
            drawCircle(color = Color(0xFFFCE4EC), radius = 10f, center = Offset(x, y)) // Outer soft pink lace
            drawCircle(color = Color.White, radius = 7.5f, center = Offset(x, y)) // Inner pure white lace
        }

        // Two cute mini lateral pink bows at the ends of the lace headband
        // Left mini bow
        val leftHeadbandBowX = centerX - 68f
        val leftHeadbandBowY = headY - 58f
        drawCircle(color = Color(0xFFFF4081), radius = 6.5f, center = Offset(leftHeadbandBowX, leftHeadbandBowY))
        drawPath(
            path = Path().apply {
                moveTo(leftHeadbandBowX, leftHeadbandBowY)
                lineTo(leftHeadbandBowX - 12f, leftHeadbandBowY - 8f)
                lineTo(leftHeadbandBowX - 12f, leftHeadbandBowY + 8f)
                close()
            },
            color = Color(0xFFFF4081)
        )
        // Right mini bow
        val rightHeadbandBowX = centerX + 68f
        val rightHeadbandBowY = headY - 58f
        drawCircle(color = Color(0xFFFF4081), radius = 6.5f, center = Offset(rightHeadbandBowX, rightHeadbandBowY))
        drawPath(
            path = Path().apply {
                moveTo(rightHeadbandBowX, rightHeadbandBowY)
                lineTo(rightHeadbandBowX + 12f, rightHeadbandBowY - 8f)
                lineTo(rightHeadbandBowX + 12f, rightHeadbandBowY + 8f)
                close()
            },
            color = Color(0xFFFF4081)
        )

        // Shimmer highlighting crown on hair top
        val shimmerHighlight = Path().apply {
            moveTo(centerX - 48f, headY - 45f)
            quadraticTo(centerX, headY - 32f, centerX + 48f, headY - 45f)
            quadraticTo(centerX, headY - 38f, centerX - 48f, headY - 45f)
            close()
        }
        drawPath(path = shimmerHighlight, color = Color.White.copy(alpha = 0.28f))
    }
}

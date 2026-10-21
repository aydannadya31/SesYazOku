package com.example.ui.util

import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

/**
 * AdaptiveButtonText
 *
 * Kural 6:
 * APK içindeki butonlardaki yazılar buton sınırları içine sığmadığı için alt satıra iniyor;
 * eğer tek kelime ise harfi alt satıra inmesi yerine sığacak kadar küçültülsün,
 * bir kelimeden fazla ise sığmayan kelime bir alt satıra insin (harf bölünmesi olmadan).
 */
@Composable
fun AdaptiveButtonText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontSize: TextUnit = 14.sp,
    fontWeight: FontWeight? = FontWeight.SemiBold,
    textAlign: TextAlign? = TextAlign.Center,
    style: TextStyle = LocalTextStyle.current
) {
    val trimmed = text.trim()
    val isSingleWord = !trimmed.contains(" ")

    if (isSingleWord) {
        // Tek kelime: Asla alt satıra harf bölme, sığması için font boyutunu küçült ve tek satırda tut
        val adaptiveSize = when {
            trimmed.length > 14 -> 9.5.sp
            trimmed.length > 10 -> 11.sp
            trimmed.length > 7 -> 12.5.sp
            else -> fontSize
        }

        Text(
            text = trimmed,
            modifier = modifier,
            color = color,
            fontSize = adaptiveSize,
            fontWeight = fontWeight,
            textAlign = textAlign,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
            style = style
        )
    } else {
        // Birden fazla kelime: Kelime bazlı alt satıra geç (LineBreak.Paragraph ile harf bölünmesini önle)
        Text(
            text = trimmed,
            modifier = modifier,
            color = color,
            fontSize = fontSize,
            fontWeight = fontWeight,
            textAlign = textAlign,
            maxLines = 2,
            softWrap = true,
            overflow = TextOverflow.Ellipsis,
            style = style.copy(
                lineBreak = LineBreak.Paragraph
            )
        )
    }
}

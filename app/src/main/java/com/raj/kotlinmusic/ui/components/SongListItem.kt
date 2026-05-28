package com.raj.kotlinmusic.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.raj.kotlinmusic.Song
import com.raj.kotlinmusic.ui.theme.ColorPalette
import androidx.compose.ui.text.style.TextOverflow

@Composable
fun SongListItem(
    song: Song,
    onClick: () -> Unit,
    theme: ColorPalette,
    dynamicAccentColor: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(theme.surface)
            .let { if (theme.id == "liquid_glass") it.border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(12.dp)) else it }
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        // Info
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = song.title,
                color = theme.text,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = song.artist,
                color = theme.mutedText,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

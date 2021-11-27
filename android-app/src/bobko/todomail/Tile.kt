package bobko.todomail

import android.content.Intent
import android.os.Build
import android.service.quicksettings.TileService
import androidx.annotation.RequiresApi
import bobko.todomail.model.StartedFrom

@RequiresApi(Build.VERSION_CODES.N)
class Tile : TileService() {
    override fun onClick() {
        startActivityAndCollapse(MainActivity.getIntent(this, StartedFrom.Tile).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        })
    }
}
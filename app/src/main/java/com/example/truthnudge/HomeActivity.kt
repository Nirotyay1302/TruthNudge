package com.example.truthnudge

// Standard Android/System Imports
import android.Manifest
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
import android.util.Log
import android.view.MenuItem
import android.widget.Button
import android.widget.Toast

// AndroidX Imports
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

// Project-specific Imports
import com.example.truthnudge.data.AppDatabase
import com.example.truthnudge.data.Claim // Your Room Entity
import com.example.truthnudge.databinding.ActivityHomeBinding
import com.example.truthnudge.ClaimItem // Assuming ClaimItem.kt is in this package

// Third-party Libraries
import com.google.android.material.navigation.NavigationView
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth

// Coroutines
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HomeActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var binding: ActivityHomeBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var db: AppDatabase
    private lateinit var adapter: ClaimsAdapter
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var toggle: ActionBarDrawerToggle

    private var btnLinkedInDev1: Button? = null
    private var btnLinkedInDev2: Button? = null

    private val requestPostNotificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            Log.d("PERMISSION", "POST_NOTIFICATIONS permission granted ✅")
            Toast.makeText(this, "Notification posting permission granted!", Toast.LENGTH_SHORT).show()
        } else {
            Log.d("PERMISSION", "POST_NOTIFICATIONS permission denied ❌")
            Toast.makeText(this, "Permission to post notifications denied.", Toast.LENGTH_LONG).show()
        }
        checkAndPromptForNotificationListenerAccess()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        db = AppDatabase.getDatabase(this)

        drawerLayout = binding.drawerLayout
        navigationView = binding.navigationView

        setupToolbarAndDrawer()
        navigationView.setNavigationItemSelectedListener(this)

        setupRecyclerView()
        setupCards()

        askForPostNotificationsPermission()
        setupLinkedInButtons()
        setupOnBackPressedHandling()
    }

    private fun setupLinkedInButtons() {
        btnLinkedInDev1 = findViewById(R.id.btn_linkedin_dev1)
        btnLinkedInDev2 = findViewById(R.id.btn_linkedin_dev2)

        btnLinkedInDev1?.setOnClickListener {
            val url = "https://www.linkedin.com/in/nirotyay-mukherjee-560632230/"
            try {
                startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
            } catch (_: ActivityNotFoundException) {
                Toast.makeText(this, "Could not open link. No browser found.", Toast.LENGTH_SHORT).show()
            }
        }

        btnLinkedInDev2?.setOnClickListener {
            val url = "https://www.linkedin.com/in/salmali-samanta-b51817284/"
            try {
                startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
            } catch (_: ActivityNotFoundException) {
                Toast.makeText(this, "Could not open link. No browser found.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupToolbarAndDrawer() {
        setSupportActionBar(binding.topAppBar)
        toggle = ActionBarDrawerToggle(
            this,
            drawerLayout,
            binding.topAppBar,
            R.string.drawer_open,
            R.string.drawer_close
        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        drawerLayout.closeDrawer(GravityCompat.START)
        when (item.itemId) {
            R.id.nav_home -> { /* Already home, or reload if needed */ }
            R.id.nav_history -> startActivity(Intent(this, HistoryActivity::class.java))
            R.id.nav_logout -> logoutUser()
            R.id.nav_settings -> promptForNotificationListenerSettings()
        }
        return true
    }

    private fun logoutUser() {
        auth.signOut()
        startActivity(Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        })
        finish()
    }

    private fun askForPostNotificationsPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    Log.i("HomeActivity_Perms", "POST_NOTIFICATIONS permission already granted.")
                    checkAndPromptForNotificationListenerAccess()
                }
                shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) -> {
                    showRationaleDialogForPostNotifications()
                }
                else -> {
                    Log.i("HomeActivity_Perms", "Requesting POST_NOTIFICATIONS permission.")
                    requestPostNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        } else {
            Log.i("HomeActivity_Perms", "POST_NOTIFICATIONS not required or implicitly granted on this API level.")
            checkAndPromptForNotificationListenerAccess()
        }
    }

    private fun showRationaleDialogForPostNotifications() {
        AlertDialog.Builder(this)
            .setTitle("Notification Permission Needed")
            .setMessage("This app shows alerts about URLs as notifications. Please grant permission to allow these alerts. This is different from Notification Access which is requested next.")
            .setPositiveButton("Grant") { _, _ ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    requestPostNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
            .setNegativeButton("No Thanks") { dialog, _ ->
                dialog.dismiss()
                Toast.makeText(this, "Alerts via notifications will be disabled.", Toast.LENGTH_LONG).show()
                checkAndPromptForNotificationListenerAccess()
            }
            .show()
    }

    private fun isNotificationListenerServiceEnabled(): Boolean {
        val packageName = packageName
        val flat = Settings.Secure.getString(contentResolver, "enabled_notification_listeners")
        if (!TextUtils.isEmpty(flat)) {
            val names = flat.split(":").toTypedArray()
            for (nameValue in names) {
                val cn = ComponentName.unflattenFromString(nameValue)
                if (cn != null) {
                    if (TextUtils.equals(packageName, cn.packageName)) {
                        Log.i("HomeActivity_Perms", "Notification Listener Service IS enabled.")
                        return true
                    }
                }
            }
        }
        Log.w("HomeActivity_Perms", "Notification Listener Service is NOT enabled.")
        return false
    }

    private fun checkAndPromptForNotificationListenerAccess() {
        if (!isNotificationListenerServiceEnabled()) {
            promptForNotificationListenerSettings(true)
        } else {
            Log.i("HomeActivity_Perms", "All necessary notification permissions/access are set!")
        }
    }

    private fun promptForNotificationListenerSettings(isInitialPrompt: Boolean = false) {
        val message = if (isInitialPrompt) {
            "To detect and analyze URLs from other apps' notifications, TruthNudge needs access to read notifications. Please enable this in system settings."
        } else {
            "Notification Access is required for TruthNudge to scan URLs from other app notifications. Please enable it in settings."
        }

        AlertDialog.Builder(this)
            .setTitle("Enable Notification Access")
            .setMessage(message)
            .setPositiveButton("Go to Settings") { _, _ ->
                try {
                    startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                } catch (e: Exception) {
                    Toast.makeText(this, "Could not open Notification Listener Settings.", Toast.LENGTH_SHORT).show()
                    Log.e("HomeActivity_Perms", "Error opening Notification Listener Settings", e)
                }
            }
            .setNegativeButton(if (isInitialPrompt) "Later" else "Cancel") { dialog, _ ->
                dialog.dismiss()
                if (isInitialPrompt) {
                    Snackbar.make(
                        binding.root,
                        "URL scanning from notifications will be disabled until access is granted.",
                        Snackbar.LENGTH_LONG
                    ).show()
                }
            }
            .setCancelable(!isInitialPrompt)
            .show()
    }

    private fun setupRecyclerView() {
        adapter = ClaimsAdapter(mutableListOf()) { clickedClaimItem: ClaimItem ->
            startActivity(Intent(this, ShareReceiverActivity::class.java).apply {
                putExtra(Intent.EXTRA_TEXT, clickedClaimItem.text)
            })
        }
        binding.recentClaimsRecycler.layoutManager = LinearLayoutManager(this)
        binding.recentClaimsRecycler.adapter = adapter
        enableSwipeToDelete()
        loadRecentClaims()
    }

    private fun setupCards() {
        binding.checkClaimCard.apply {
            translationY = -300f
            alpha = 0f
            animate().translationY(0f).alpha(1f).setDuration(500).start()
            setOnClickListener {
                startActivity(Intent(this@HomeActivity, ShareReceiverActivity::class.java))
            }
        }

        binding.historyCard.apply {
            translationY = -300f
            alpha = 0f
            animate().translationY(0f).alpha(1f).setDuration(600).start()
            setOnClickListener {
                startActivity(Intent(this@HomeActivity, HistoryActivity::class.java))
            }
        }
    }

    override fun onStart() {
        super.onStart()
        if (auth.currentUser == null) {
            startActivity(Intent(this, LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            })
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        Log.d("HomeActivity_Perms", "onResume: Checking listener status.")
        if (!isNotificationListenerServiceEnabled()) {
            Log.w("HomeActivity_Perms", "onResume: Listener is NOT enabled.")
        } else {
            Log.i("HomeActivity_Perms", "onResume: Listener IS enabled.")
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (toggle.onOptionsItemSelected(item)) {
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun setupOnBackPressedHandling() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START)
                } else {
                    if (isEnabled) {
                        isEnabled = false
                        onBackPressedDispatcher.onBackPressed()
                    }
                }
            }
        })
    }

    private fun loadRecentClaims() {
        CoroutineScope(Dispatchers.IO).launch {
            val recentDbClaims: List<Claim> = db.claimDao().getLastNClaims(5)
            val recentClaimItems = recentDbClaims.map { dbClaim ->
                // ASSUMPTION: dbClaim.id is Long, dbClaim.timestamp is Long
                ClaimItem(
                    id = dbClaim.id,
                    text = "${dbClaim.text}\nVerdict: ${dbClaim.verdict}",
                    timestamp = dbClaim.timestamp
                )
            }
            withContext(Dispatchers.Main) {
                adapter.updateClaims(recentClaimItems)
            }
        }
    }

    private fun enableSwipeToDelete() {
        val callback =
            object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
                override fun onMove(
                    recyclerView: RecyclerView,
                    vh: RecyclerView.ViewHolder,
                    target: RecyclerView.ViewHolder
                ): Boolean = false

                override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                    val position = viewHolder.adapterPosition
                    // Added more robust bounds check for claimList access
                    if (position == RecyclerView.NO_POSITION || position >= adapter.claimList.size) return

                    val deletedClaimItem = adapter.claimList[position]

                    CoroutineScope(Dispatchers.IO).launch {
                        // ASSUMPTION: db.claimDao().getClaimById(id: Long) exists and returns Claim?
                        val claimEntityToDelete = db.claimDao().getClaimById(deletedClaimItem.id)

                        claimEntityToDelete?.let { db.claimDao().deleteClaim(it) }

                        val undoAction = {
                            CoroutineScope(Dispatchers.IO).launch {
                                claimEntityToDelete?.let { originalClaimEntity ->
                                    db.claimDao().insertClaim(originalClaimEntity)
                                    withContext(Dispatchers.Main) {
                                        // ASSUMPTION: adapter.addClaimAt(position: Int, item: ClaimItem) exists
                                        adapter.addClaimAt(position, deletedClaimItem)
                                    }
                                }
                            }
                        }

                        withContext(Dispatchers.Main) {
                            // ASSUMPTION: adapter.removeClaimAt(position: Int) exists
                            adapter.removeClaimAt(position)

                            Snackbar.make(binding.recentClaimsRecycler, "Claim deleted", Snackbar.LENGTH_LONG)
                                .setAction("UNDO") {
                                    undoAction()
                                }.show()
                        }
                    }
                }
            }
        ItemTouchHelper(callback).attachToRecyclerView(binding.recentClaimsRecycler)
    }
}

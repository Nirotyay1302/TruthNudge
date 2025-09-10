package com.example.truthnudge

import android.Manifest
import android.content.ActivityNotFoundException // Added for LinkedIn link
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
import android.util.Log
import android.view.MenuItem
import android.widget.Button // Added for LinkedIn buttons
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat // Added for closing drawer
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.truthnudge.data.AppDatabase
import com.example.truthnudge.data.Claim
import com.example.truthnudge.databinding.ActivityHomeBinding
import com.google.android.material.navigation.NavigationView
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HomeActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener { // Implement listener

    private lateinit var binding: ActivityHomeBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var db: AppDatabase
    private lateinit var adapter: ClaimsAdapter
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var toggle: ActionBarDrawerToggle
    private lateinit var recentClaimsRecycler: RecyclerView

    // LinkedIn Buttons (declare them here)
    private var btnLinkedInDev1: Button? = null
    private var btnLinkedInDev2: Button? = null

    private val requestPostNotificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            Log.d("PERMISSION", "POST_NOTIFICATIONS permission granted ✅")
            Toast.makeText(this, "Notification posting permission granted!", Toast.LENGTH_SHORT).show()
            checkAndPromptForNotificationListenerAccess()
        } else {
            Log.d("PERMISSION", "POST_NOTIFICATIONS permission denied ❌")
            Toast.makeText(this, "Permission to post notifications denied.", Toast.LENGTH_LONG).show()
            checkAndPromptForNotificationListenerAccess()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root) // binding.root is the root of activity_home.xml

        auth = FirebaseAuth.getInstance()
        db = AppDatabase.getDatabase(this)

        drawerLayout = binding.drawerLayout
        navigationView = binding.navigationView

        setupToolbarAndDrawer()
        // setupNavigationMenu() is now part of onNavigationItemSelected setup below
        navigationView.setNavigationItemSelectedListener(this) // Set listener

        recentClaimsRecycler = binding.recentClaimsRecycler
        setupRecyclerView()
        setupCards()

        askForPostNotificationsPermission()

        // ***** START OF LINKEDIN BUTTON SETUP *****
        // Assuming btn_linkedin_dev1 and btn_linkedin_dev2 are IDs in layout_drawer_connect_developers.xml
        // which is included in activity_home.xml
        btnLinkedInDev1 = findViewById(R.id.btn_linkedin_dev1)
        btnLinkedInDev2 = findViewById(R.id.btn_linkedin_dev2)

        btnLinkedInDev1?.setOnClickListener {
            val url = "https://www.linkedin.com/in/nirotyay-mukherjee-560632230/" // *** REPLACE WITH ACTUAL URL ***
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                startActivity(intent)
            } catch (e: ActivityNotFoundException) {
                Toast.makeText(this, "Could not open link. No browser found.", Toast.LENGTH_SHORT).show()
            }
        }

        btnLinkedInDev2?.setOnClickListener {
            val url = "https://www.linkedin.com/in/salmali-samanta-b51817284/" // *** REPLACE WITH ACTUAL URL ***
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                startActivity(intent)
            } catch (e: ActivityNotFoundException) {
                Toast.makeText(this, "Could not open link. No browser found.", Toast.LENGTH_SHORT).show()
            }
        }
        // ***** END OF LINKEDIN BUTTON SETUP *****
    }

    private fun setupToolbarAndDrawer() {
        setSupportActionBar(binding.topAppBar) // Use binding for toolbar
        // supportActionBar?.setDisplayHomeAsUpEnabled(false) // Not needed if using toggle for home
        toggle = ActionBarDrawerToggle(
            this,
            drawerLayout,
            binding.topAppBar, // Use binding for toolbar
            R.string.drawer_open,
            R.string.drawer_close
        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()
        // It's common to enable the hamburger icon to open the drawer
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    // This method is now replaced by onNavigationItemSelected
    // private fun setupNavigationMenu() { ... }


    // Handle NavigationView item selections
    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        drawerLayout.closeDrawer(GravityCompat.START) // Close drawer first
        when (item.itemId) {
            R.id.nav_home -> { /* Already home, or reload if needed */ }
            R.id.nav_history -> startActivity(Intent(this, HistoryActivity::class.java))
            R.id.nav_logout -> logoutUser()
            R.id.nav_settings -> promptForNotificationListenerSettings()
            // Add other menu item handling here if you have them
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

    private fun openNotificationSettings() {
        promptForNotificationListenerSettings(false)
    }

    private fun setupRecyclerView() {
        adapter = ClaimsAdapter(mutableListOf()) { claimText ->
            startActivity(Intent(this, ShareReceiverActivity::class.java).apply {
                putExtra(Intent.EXTRA_TEXT, claimText)
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

    // This makes the hamburger icon work
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (toggle.onOptionsItemSelected(item)) {
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    // Added to handle back press when drawer is open
    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    private fun loadRecentClaims() {
        CoroutineScope(Dispatchers.IO).launch {
            val recentClaims: List<Claim> = db.claimDao().getLastNClaims(5)
            withContext(Dispatchers.Main) {
                adapter.updateClaims(recentClaims.map { "${it.text}\nVerdict: ${it.verdict}" })
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
                ) = false

                override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                    val position = viewHolder.adapterPosition
                    if (position == RecyclerView.NO_POSITION) return

                    val deletedClaimText = adapter.claimList[position]

                    CoroutineScope(Dispatchers.IO).launch {
                        val claimToDelete = db.claimDao().getAllClaims().find {
                            "${it.text}\nVerdict: ${it.verdict}" == deletedClaimText
                        }
                        claimToDelete?.let { db.claimDao().deleteClaim(it) }

                        val undoAction = {
                            CoroutineScope(Dispatchers.IO).launch {
                                claimToDelete?.let { originalClaim ->
                                    db.claimDao().insertClaim(originalClaim)
                                    withContext(Dispatchers.Main) {
                                        val currentList = adapter.claimList.toMutableList()
                                        if (position <= currentList.size) {
                                            currentList.add(position, deletedClaimText)
                                            adapter.updateClaims(currentList)
                                        } else {
                                            currentList.add(deletedClaimText)
                                            adapter.updateClaims(currentList)
                                        }
                                    }
                                }
                            }
                        }

                        withContext(Dispatchers.Main) {
                            val updatedList = adapter.claimList.toMutableList().apply { removeAt(position) }
                            adapter.updateClaims(updatedList)

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

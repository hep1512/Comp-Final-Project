package com.supermarket.routes

import com.supermarket.data.hashPassword
import com.supermarket.data.userStore
import com.supermarket.models.LoginRequest
import com.supermarket.models.Role
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*
import com.supermarket.models.UserSession

fun Application.registerRoutes() {
    routing {

        // Public endpoints ye
        get("/") {
            call.respondRedirect("/login")
        }

        get("/login") {
            val session = call.sessions.get<UserSession>()
            if (session != null) {
                call.respondRedirect("/dashboard")
                return@get
            }
            call.respondText(loginPageHtml(), ContentType.Text.Html)
        }

        post("/login") {
            val params = call.receiveParameters()
            val username = params["username"] ?: ""
            val password = params["password"] ?: ""
            val user = userStore[username]

            if (user == null || user.passwordHash != hashPassword(password)) {
                call.respondText(loginPageHtml("Invalid username or password."), ContentType.Text.Html)
                return@post
            }

            call.sessions.set(UserSession(username = user.username, role = user.role.name))
            call.respondRedirect("/dashboard")
        }

        get("/logout") {
            call.sessions.clear<UserSession>()
            call.respondRedirect("/login")
        }

        // USER, EMPLOYEE, ADMIN
        get("/dashboard") {
            val session = call.sessions.get<UserSession>() ?: return@get call.respondRedirect("/login")
            call.respondText(dashboardHtml(session), ContentType.Text.Html)
        }

        // EMPLOYEE, + ADMIN
        get("/inventory") {
            val session = call.sessions.get<UserSession>() ?: return@get call.respondRedirect("/login")
            requireRole(call, session, Role.EMPLOYEE, Role.ADMIN) {
                call.respondText(inventoryHtml(session), ContentType.Text.Html)
            }
        }

        // ADMIN
        get("/admin") {
            val session = call.sessions.get<UserSession>() ?: return@get call.respondRedirect("/login")
            requireRole(call, session, Role.ADMIN) {
                call.respondText(adminHtml(session), ContentType.Text.Html)
            }
        }

        // ADMIN only
        get("/admin/users") {
            val session = call.sessions.get<UserSession>() ?: return@get call.respondRedirect("/login")
            requireRole(call, session, Role.ADMIN) {
                val userList = userStore.values.joinToString("") {
                    "<tr><td>${it.username}</td><td>${it.role}</td></tr>"
                }
                call.respondText(adminUsersHtml(session, userList), ContentType.Text.Html)
            }
        }
    }
}

private suspend fun requireRole(
    call: ApplicationCall,
    session: UserSession,
    vararg allowed: Role,
    block: suspend () -> Unit
) {
    if (Role.valueOf(session.role) in allowed) {
        block()
    } else {
        call.respondText(forbiddenHtml(session), ContentType.Text.Html, HttpStatusCode.Forbidden)
    }
}

private fun nav(session: UserSession) = """
    <nav>
        <a href="/dashboard">Dashboard</a>
        ${if (session.role == "EMPLOYEE" || session.role == "ADMIN") "<a href=\"/inventory\">Inventory</a>" else ""}
        ${if (session.role == "ADMIN") "<a href=\"/admin\">Admin</a> <a href=\"/admin/users\">Users</a>" else ""}
        <a href="/logout">Logout</a>
    </nav>
"""

private fun loginPageHtml(error: String = "") = """
<!DOCTYPE html>
<html>
<head>
    <title>Supermarket Login</title>
    <style>
        body { font-family: sans-serif; display:flex; justify-content:center; align-items:center; height:100vh; margin:0; background:#f0f0f0; }
        .box { background:white; padding:2rem; border-radius:8px; box-shadow:0 2px 8px rgba(0,0,0,.2); width:300px; }
        h1 { margin-top:0; }
        input { width:100%; padding:.5rem; margin:.4rem 0 1rem; box-sizing:border-box; border:1px solid #ccc; border-radius:4px; }
        button { width:100%; padding:.6rem; background:#2e7d32; color:white; border:none; border-radius:4px; cursor:pointer; font-size:1rem; }
        button:hover { background:#1b5e20; }
        .error { margin-top:1rem; color:red; font-size:.9rem; }
    </style>
</head>
<body>
<div class="box">
    <h1>super market login thing</h1>
    <form method="post" action="/login">
        <input type="text"     name="username" placeholder="Username" required />
        <input type="password" name="password" placeholder="Password" required />
        <button type="submit">Login</button>
    </form>
    ${if (error.isNotEmpty()) "<p class=\"error\">$error</p>" else ""}
</div>
</body>
</html>
""".trimIndent()

private fun dashboardHtml(session: UserSession) = """
<!DOCTYPE html><html><head><title>Dashboard</title>
<style>body{font-family:sans-serif;padding:2rem;} nav{margin-bottom:1rem;} a{margin-right:1rem;}</style>
</head><body>
${nav(session)}
<h1>Welcome, ${session.username}</h1>
<p>Role: <strong>${session.role}</strong></p>
</body></html>
""".trimIndent()

private fun inventoryHtml(session: UserSession) = """
<!DOCTYPE html><html><head><title>Inventory</title>
<style>body{font-family:sans-serif;padding:2rem;} nav{margin-bottom:1rem;} a{margin-right:1rem;}</style>
</head><body>
${nav(session)}
<h1>Inventory</h1>
<p>Stock management — visible to employees and admins.</p>
</body></html>
""".trimIndent()

private fun adminHtml(session: UserSession) = """
<!DOCTYPE html><html><head><title>Admin</title>
<style>body{font-family:sans-serif;padding:2rem;} nav{margin-bottom:1rem;} a{margin-right:1rem;}</style>
</head><body>
${nav(session)}
<h1>Admin Panel</h1>
<p>Administrative controls — admins only.</p>
</body></html>
""".trimIndent()

private fun adminUsersHtml(session: UserSession, userList: String) = """
<!DOCTYPE html><html><head><title>Users</title>
<style>body{font-family:sans-serif;padding:2rem;} nav{margin-bottom:1rem;} a{margin-right:1rem;}
table{border-collapse:collapse;width:300px;} td,th{border:1px solid #ccc;padding:.5rem;text-align:left;}</style>
</head><body>
${nav(session)}
<h1>Users</h1>
<table><thead><tr><th>Username</th><th>Role</th></tr></thead>
<tbody>$userList</tbody></table>
</body></html>
""".trimIndent()

private fun forbiddenHtml(session: UserSession) = """
<!DOCTYPE html><html><head><title>Access Denied</title>
<style>body{font-family:sans-serif;padding:2rem;} nav{margin-bottom:1rem;} a{margin-right:1rem;}</style>
</head><body>
${nav(session)}
<h1>403 — Access Denied</h1>
<p>You don't have permission to view this page.</p>
</body></html>
""".trimIndent()

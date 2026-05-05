package com.supermarket.routes

import com.supermarket.data.allProducts
import com.supermarket.data.findProduct
import com.supermarket.data.getCart
import com.supermarket.data.getOrders
import com.supermarket.models.Product
import com.supermarket.models.UserSession
import com.supermarket.repositories.MarketingDashboardStats
import com.supermarket.repositories.ProductRecommendation
import com.supermarket.data.lowStockCount
import com.supermarket.data.outOfStockCount
import com.supermarket.data.totalOrdersCount
import com.supermarket.data.totalProductsCount
import com.supermarket.data.totalSalesAmount
import java.util.Locale

private data class DashboardChartRow(
    val label: String,
    val valueLabel: String,
    val value: Double
)

fun nav(session: UserSession, currentPage: String = "") = """
    <nav class="topbar">
        <div class="topbar-inner">
            <div class="brand">
                <a href="/dashboard">SuperMarket</a>
            </div>

            <div class="nav-links">
                <a class="${if (currentPage == "dashboard") "active" else ""}" href="/dashboard">Dashboard</a>
                <a class="${if (currentPage == "products") "active" else ""}" href="/products">Products</a>
                <a class="${if (currentPage == "cart") "active" else ""}" href="/cart">Cart</a>
                <a class="${if (currentPage == "orders") "active" else ""}" href="/orders">Orders</a>
                ${if (session.role == "EMPLOYEE" || session.role == "ADMIN") "<a class=\"${if (currentPage == "inventory") "active" else ""}\" href=\"/inventory\">Inventory</a>" else ""}
                ${if (session.role == "ADMIN") "<a class=\"${if (currentPage == "admin") "active" else ""}\" href=\"/admin\">Admin</a>" else ""}
                ${if (session.role == "ADMIN") "<a class=\"${if (currentPage == "users") "active" else ""}\" href=\"/admin/users\">Users</a>" else ""}
                ${if (session.role == "ADMIN") "<a class=\"${if (currentPage == "analytics") "active" else ""}\" href=\"/analytics\">Analytics</a>" else ""}
            </div>

            <div class="nav-right">
                <span class="user-badge">${session.username} (${session.role})</span>
                <a class="logout-btn" href="/logout">Logout</a>
            </div>
        </div>
    </nav>
""".trimIndent()

fun commonStyles() = """
    <style>
        * {
            box-sizing: border-box;
        }

        body {
            font-family: Arial, sans-serif;
            margin: 0;
            background: #f6f8fb;
            color: #222;
        }

        .topbar {
            background: #1f2937;
            box-shadow: 0 2px 10px rgba(0,0,0,0.12);
            position: sticky;
            top: 0;
            z-index: 1000;
        }

        .topbar-inner {
            max-width: 1200px;
            margin: 0 auto;
            min-height: 72px;
            padding: 0 2rem;
            display: flex;
            align-items: center;
            justify-content: space-between;
            gap: 1.5rem;
        }

        .brand a {
            color: white;
            text-decoration: none;
            font-size: 1.4rem;
            font-weight: 700;
            letter-spacing: 0.3px;
        }

        .nav-links {
            display: flex;
            align-items: center;
            gap: 0.4rem;
            flex-wrap: wrap;
        }

        .nav-links a {
            color: #dbe4ee;
            text-decoration: none;
            padding: 0.65rem 1rem;
            border-radius: 10px;
            font-weight: 500;
            transition: background 0.2s ease, color 0.2s ease;
        }

        .nav-links a:hover {
            background: rgba(255,255,255,0.10);
            color: white;
        }

        .nav-links a.active {
            background: #2563eb;
            color: white;
        }

        .nav-right {
            display: flex;
            align-items: center;
            gap: 0.75rem;
            flex-wrap: wrap;
        }

        .user-badge {
            color: #e5e7eb;
            font-size: 0.95rem;
            background: rgba(255,255,255,0.08);
            padding: 0.45rem 0.8rem;
            border-radius: 999px;
            white-space: nowrap;
        }

        .logout-btn {
            background: #dc2626;
            color: white;
            text-decoration: none;
            padding: 0.65rem 1rem;
            border-radius: 10px;
            font-weight: 600;
            transition: background 0.2s ease;
        }

        .logout-btn:hover {
            background: #b91c1c;
        }

        .container {
            max-width: 1100px;
            margin: 0 auto;
            padding: 2rem;
        }

        h1, h2, h3 {
            margin-top: 0;
        }

        .card-grid {
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
            gap: 1rem;
        }

        .card {
            background: white;
            border-radius: 12px;
            padding: 1.25rem;
            box-shadow: 0 2px 8px rgba(0,0,0,0.08);
        }

        .btn, button {
            display: inline-block;
            background: #2e7d32;
            color: white;
            text-decoration: none;
            border: none;
            border-radius: 8px;
            padding: 0.7rem 1rem;
            cursor: pointer;
            font-size: 0.95rem;
        }

        .btn.secondary {
            background: #1565c0;
        }

        .muted {
            color: #666;
        }

        .tag {
            display: inline-block;
            background: #e8f5e9;
            color: #1b5e20;
            border-radius: 999px;
            padding: 0.25rem 0.75rem;
            font-size: 0.85rem;
            margin-top: 0.5rem;
        }

        table {
            width: 100%;
            border-collapse: collapse;
            background: white;
            border-radius: 12px;
            overflow: hidden;
            box-shadow: 0 2px 8px rgba(0,0,0,0.08);
        }

        th, td {
            padding: 0.9rem 1rem;
            border-bottom: 1px solid #e5e7eb;
            text-align: left;
        }

        th {
            background: #f3f4f6;
        }

        .summary-box {
            background: white;
            border-radius: 12px;
            padding: 1rem;
            margin-top: 1rem;
            box-shadow: 0 2px 8px rgba(0,0,0,0.08);
        }

        .form-box {
            background: white;
            border-radius: 12px;
            padding: 1.5rem;
            max-width: 650px;
            box-shadow: 0 2px 8px rgba(0,0,0,0.08);
        }

        input, select {
            width: 100%;
            padding: 0.7rem;
            margin: 0.4rem 0 1rem;
            box-sizing: border-box;
            border: 1px solid #d1d5db;
            border-radius: 8px;
        }

        .actions {
            display: flex;
            gap: 0.75rem;
            flex-wrap: wrap;
            margin-top: 1rem;
        }

        .hero {
            background: white;
            padding: 1.5rem;
            border-radius: 14px;
            box-shadow: 0 2px 8px rgba(0,0,0,0.08);
            margin-bottom: 1.5rem;
        }

        .marketing-dashboard {
            margin-top: 1.5rem;
        }

        .marketing-header {
            display: flex;
            align-items: center;
            justify-content: space-between;
            gap: 1rem;
            flex-wrap: wrap;
            margin-bottom: 1rem;
        }

        .section-kicker {
            margin: 0 0 0.35rem;
            color: #2563eb;
            font-size: 0.78rem;
            font-weight: 700;
            letter-spacing: 0.08em;
            text-transform: uppercase;
        }

        .metric-grid {
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(190px, 1fr));
            gap: 1rem;
            margin-bottom: 1rem;
        }

        .metric-card,
        .chart-card {
            background: white;
            border-radius: 12px;
            padding: 1rem;
            box-shadow: 0 2px 8px rgba(0,0,0,0.08);
        }

        .metric-card {
            min-height: 118px;
        }

        .metric-label,
        .metric-note,
        .column-value {
            color: #64748b;
            font-size: 0.9rem;
        }

        .metric-value {
            margin: 0.35rem 0;
            color: #0f172a;
            font-size: 1.7rem;
            font-weight: 700;
            line-height: 1.15;
            overflow-wrap: anywhere;
        }

        .chart-grid {
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
            gap: 1rem;
        }

        .chart-title-row {
            display: flex;
            align-items: baseline;
            justify-content: space-between;
            gap: 0.75rem;
            margin-bottom: 1rem;
        }

        .chart-title-row h3 {
            margin: 0;
            font-size: 1.05rem;
        }

        .bar-chart {
            display: grid;
            gap: 0.85rem;
        }

        .bar-row {
            display: grid;
            gap: 0.4rem;
        }

        .bar-label {
            display: flex;
            justify-content: space-between;
            gap: 0.75rem;
            font-size: 0.92rem;
        }

        .bar-label span {
            overflow-wrap: anywhere;
        }

        .bar-label strong {
            white-space: nowrap;
        }

        .bar-track {
            height: 14px;
            overflow: hidden;
            border-radius: 999px;
            background: #e5e7eb;
        }

        .bar-fill {
            height: 100%;
            border-radius: inherit;
            background: linear-gradient(90deg, #059669, #2563eb);
        }

        .column-chart {
            min-height: 230px;
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(70px, 1fr));
            align-items: end;
            gap: 0.85rem;
        }

        .column-item {
            min-width: 0;
            display: grid;
            grid-template-rows: 1fr auto auto;
            gap: 0.45rem;
            align-items: end;
            text-align: center;
        }

        .column-plot {
            height: 150px;
            display: flex;
            align-items: flex-end;
            justify-content: center;
            border-radius: 8px;
            background: #eef2ff;
            padding: 0.45rem;
        }

        .column-fill {
            width: 100%;
            min-height: 12px;
            border-radius: 7px 7px 4px 4px;
            background: linear-gradient(180deg, #f59e0b, #0ea5e9);
        }

        .column-label {
            min-height: 2.5rem;
            color: #1f2937;
            font-size: 0.86rem;
            font-weight: 600;
            overflow-wrap: anywhere;
        }

        .empty-chart {
            border: 1px dashed #cbd5e1;
            border-radius: 10px;
            color: #64748b;
            padding: 1rem;
            text-align: center;
        }

        .recommendation-grid {
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
            gap: 1rem;
        }

        .recommendation-card {
            min-height: 330px;
            display: flex;
            flex-direction: column;
        }

        .recommendation-head {
            display: flex;
            justify-content: space-between;
            gap: 0.75rem;
            align-items: flex-start;
        }

        .recommendation-head h3 {
            margin-bottom: 0.35rem;
        }

        .score-pill {
            flex: 0 0 auto;
            background: #eff6ff;
            border: 1px solid #bfdbfe;
            border-radius: 999px;
            color: #1d4ed8;
            font-size: 0.82rem;
            font-weight: 700;
            padding: 0.3rem 0.6rem;
            white-space: nowrap;
        }

        .score-meter {
            height: 8px;
            overflow: hidden;
            border-radius: 999px;
            background: #e5e7eb;
            margin: 0.8rem 0;
        }

        .score-meter span {
            display: block;
            height: 100%;
            border-radius: inherit;
            background: linear-gradient(90deg, #22c55e, #0ea5e9);
        }

        .recommendation-reasons {
            display: flex;
            flex-wrap: wrap;
            gap: 0.45rem;
            margin: 0.75rem 0 1rem;
        }

        .reason-chip {
            background: #f1f5f9;
            border-radius: 999px;
            color: #334155;
            font-size: 0.8rem;
            padding: 0.3rem 0.55rem;
        }

        .recommendation-card .actions {
            margin-top: auto;
        }

        @media (max-width: 900px) {
            .topbar-inner {
                flex-direction: column;
                align-items: flex-start;
                padding: 1rem 1.25rem;
            }

            .nav-links,
            .nav-right {
                width: 100%;
            }

            .container {
                padding: 1.25rem;
            }
        }
    </style>
""".trimIndent()

fun loginPageHtml(error: String = "") = """
<!DOCTYPE html>
<html>
<head>
    <title>Supermarket Login</title>
    <style>
        body {
            font-family: Arial, sans-serif;
            display:flex;
            justify-content:center;
            align-items:center;
            height:100vh;
            margin:0;
            background:#f0f4f8;
        }
        .box {
            background:white;
            padding:2rem;
            border-radius:12px;
            box-shadow:0 2px 10px rgba(0,0,0,.15);
            width:320px;
        }
        h1 { margin-top:0; }
        input {
            width:100%;
            padding:.7rem;
            margin:.4rem 0 1rem;
            box-sizing:border-box;
            border:1px solid #ccc;
            border-radius:8px;
        }
        button {
            width:100%;
            padding:.8rem;
            background:#2e7d32;
            color:white;
            border:none;
            border-radius:8px;
            cursor:pointer;
            font-size:1rem;
        }
        .error { margin-top:1rem; color:red; font-size:.9rem; }
    </style>
</head>
<body>
<div class="box">
    <h1>Supermarket Login</h1>
    <form method="post" action="/login">
        <input type="text" name="username" placeholder="Username" required />
        <input type="password" name="password" placeholder="Password" required />
        <button type="submit">Login</button>
    </form>
    ${if (error.isNotEmpty()) "<p class=\"error\">$error</p>" else ""}
    <p style="margin-top:1rem; text-align:center; font-size:.9rem;">
    Don't have an account? <a href="/register" style="color:#2e7d32;">Register</a>
    </p>
</div>
</body>
</html>
""".trimIndent()

private fun formatMoney(value: Double) = "&pound;${String.format(Locale.UK, "%.2f", value)}"

private fun formatOneDecimal(value: Double) = String.format(Locale.UK, "%.1f", value)

private fun horizontalBarChart(
    title: String,
    rows: List<DashboardChartRow>,
    emptyMessage: String
): String {
    val positiveRows = rows.filter { it.value > 0.0 }
    if (positiveRows.isEmpty()) {
        return """
        <div class="chart-card">
            <div class="chart-title-row">
                <h3>$title</h3>
            </div>
            <div class="empty-chart">$emptyMessage</div>
        </div>
        """
    }

    val maxValue = positiveRows.maxOf { it.value }
    val chartRows = positiveRows.joinToString("") { row ->
        val percent = ((row.value / maxValue) * 100).coerceIn(8.0, 100.0).toInt()
        """
        <div class="bar-row">
            <div class="bar-label">
                <span>${row.label}</span>
                <strong>${row.valueLabel}</strong>
            </div>
            <div class="bar-track">
                <div class="bar-fill" style="width: $percent%;"></div>
            </div>
        </div>
        """
    }

    return """
    <div class="chart-card">
        <div class="chart-title-row">
            <h3>$title</h3>
        </div>
        <div class="bar-chart">
            $chartRows
        </div>
    </div>
    """
}

private fun columnChart(
    title: String,
    rows: List<DashboardChartRow>,
    emptyMessage: String
): String {
    val positiveRows = rows.filter { it.value > 0.0 }
    if (positiveRows.isEmpty()) {
        return """
        <div class="chart-card">
            <div class="chart-title-row">
                <h3>$title</h3>
            </div>
            <div class="empty-chart">$emptyMessage</div>
        </div>
        """
    }

    val maxValue = positiveRows.maxOf { it.value }
    val columns = positiveRows.joinToString("") { row ->
        val height = ((row.value / maxValue) * 100).coerceIn(12.0, 100.0).toInt()
        """
        <div class="column-item">
            <div class="column-plot">
                <div class="column-fill" style="height: $height%;"></div>
            </div>
            <div class="column-label">${row.label}</div>
            <div class="column-value">${row.valueLabel}</div>
        </div>
        """
    }

    return """
    <div class="chart-card">
        <div class="chart-title-row">
            <h3>$title</h3>
        </div>
        <div class="column-chart">
            $columns
        </div>
    </div>
    """
}

private fun employeeMarketingDashboardHtml(stats: MarketingDashboardStats): String {
    val mostPopular = stats.bestSellers.firstOrNull()?.name ?: "No sales yet"
    val strongestCategory = stats.categorySales.firstOrNull()?.category ?: "No sales yet"
    val topProductRows = stats.bestSellers.take(5).map {
        DashboardChartRow(it.name, "${it.unitsSold} sold", it.unitsSold.toDouble())
    }
    val categoryRows = stats.categorySales.take(5).map {
        DashboardChartRow(it.category, formatMoney(it.revenue), it.revenue)
    }
    val spendBandRows = stats.orderSpendBands.map {
        DashboardChartRow(it.label, "${it.orderCount} orders", it.orderCount.toDouble())
    }

    return """
    <section class="marketing-dashboard">
        <div class="marketing-header">
            <div>
                <p class="section-kicker">Employee Marketing Dashboard</p>
                <h2>Sales Performance</h2>
            </div>
            <a class="btn secondary" href="/inventory">Manage Inventory</a>
        </div>

        <div class="metric-grid">
            <div class="metric-card">
                <div class="metric-label">Total Revenue</div>
                <div class="metric-value">${formatMoney(stats.totalRevenue)}</div>
                <div class="metric-note">${stats.totalOrders} completed orders</div>
            </div>

            <div class="metric-card">
                <div class="metric-label">Average Spend</div>
                <div class="metric-value">${formatMoney(stats.averageOrderValue)}</div>
                <div class="metric-note">Per order</div>
            </div>

            <div class="metric-card">
                <div class="metric-label">Average Basket</div>
                <div class="metric-value">${formatOneDecimal(stats.averageItemsPerOrder)}</div>
                <div class="metric-note">${stats.totalUnitsSold} units sold</div>
            </div>

            <div class="metric-card">
                <div class="metric-label">Top Product</div>
                <div class="metric-value">$mostPopular</div>
                <div class="metric-note">Top category: $strongestCategory</div>
            </div>
        </div>

        <div class="chart-grid">
            ${horizontalBarChart("Most Popular Products", topProductRows, "No product sales yet.")}
            ${columnChart("Revenue by Category", categoryRows, "No category sales yet.")}
            ${horizontalBarChart("Order Spend Bands", spendBandRows, "No completed orders yet.")}
        </div>
    </section>
    """
}

private fun customerRecommendationsHtml(recommendations: List<ProductRecommendation>): String {
    if (recommendations.isEmpty()) return ""

    val cards = recommendations.joinToString("") { recommendation ->
        val product = recommendation.product
        val reasonChips = recommendation.reasons.joinToString("") {
            "<span class=\"reason-chip\">$it</span>"
        }

        """
        <div class="card recommendation-card">
            <div class="recommendation-head">
                <div>
                    <h3>${product.name}</h3>
                    <p class="muted">Category: ${product.category}</p>
                </div>
                <span class="score-pill">${recommendation.matchPercent}% match</span>
            </div>

            <p><strong>${formatMoney(product.price)}</strong></p>
            <div class="score-meter">
                <span style="width: ${recommendation.matchPercent}%;"></span>
            </div>
            <div class="recommendation-reasons">
                $reasonChips
            </div>
            <span class="tag">${product.stock}</span>

            <div class="actions">
                <a class="btn secondary" href="/products/${product.id}">View Details</a>
                <form method="post" action="/cart/add/${product.id}" style="display:inline;">
                    <input type="hidden" name="quantity" value="1" />
                    <button class="btn" type="submit">Add to Cart</button>
                </form>
            </div>
        </div>
        """
    }

    return """
    <section class="marketing-dashboard">
        <div class="marketing-header">
            <div>
                <p class="section-kicker">Recommended For You</p>
                <h2>Products You Might Like</h2>
            </div>
            <a class="btn secondary" href="/products">Browse All Products</a>
        </div>

        <div class="recommendation-grid">
            $cards
        </div>
    </section>
    """
}

fun dashboardHtml(
    session: UserSession,
    marketingStats: MarketingDashboardStats? = null,
    recommendations: List<ProductRecommendation> = emptyList()
) = """
<!DOCTYPE html>
<html>
<head>
    <title>Dashboard</title>
    ${commonStyles()}
</head>
<body>
${nav(session, "dashboard")}
<div class="container">
    <div class="hero">
        <h1>Welcome, ${session.username}</h1>
        <p class="muted">Role: <strong>${session.role}</strong></p>
        <p>This is the main entry point of the supermarket system.</p>
    </div>

    <div class="card-grid">
        <div class="card">
            <h3>Browse Products</h3>
            <p class="muted">View available supermarket items and open product details.</p>
            <a class="btn" href="/products">Go to Products</a>
        </div>

        <div class="card">
            <h3>My Cart</h3>
            <p class="muted">Review selected items and continue to checkout.</p>
            <a class="btn secondary" href="/cart">Open Cart</a>
        </div>

        <div class="card">
            <h3>My Orders</h3>
            <p class="muted">Check order history and current order status.</p>
            <a class="btn" href="/orders">View Orders</a>
        </div>
    </div>

    ${if (marketingStats != null) employeeMarketingDashboardHtml(marketingStats) else ""}
    ${if (marketingStats == null) customerRecommendationsHtml(recommendations) else ""}
</div>
</body>
</html>
""".trimIndent()

fun productsHtml(session: UserSession): String {
    val productCards = allProducts().joinToString("") { product ->
        """
        <div class="card">
            <h3>${product.name}</h3>
            <p class="muted">Category: ${product.category}</p>
            <p><strong>£${"%.2f".format(product.price)}</strong></p>
            <span class="tag">${product.stock}</span>
            <div class="actions">
                <a class="btn secondary" href="/products/${product.id}">View Details</a>
                <form method="post" action="/cart/add/${product.id}" style="display:inline;">
                    <input type="hidden" name="quantity" value="1" />
                    <button class="btn" type="submit">Add to Cart</button>
                </form>
            </div>
        </div>
        """
    }

    return """
<!DOCTYPE html>
<html>
<head>
    <title>Products</title>
    ${commonStyles()}
</head>
<body>
${nav(session, "products")}
<div class="container">
    <h1>Products</h1>
    <p class="muted">Browse available items in the supermarket.</p>
    <div class="card-grid">
        $productCards
    </div>
</div>
</body>
</html>
""".trimIndent()
}

fun productDetailHtml(session: UserSession, product: Product) = """
<!DOCTYPE html>
<html>
<head>
    <title>${product.name}</title>
    ${commonStyles()}
</head>
<body>
${nav(session, "products")}
<div class="container">
    <div class="card">
        <h1>${product.name}</h1>
        <p class="muted">Category: ${product.category}</p>
        <p><strong>Price: £${"%.2f".format(product.price)}</strong></p>
        <p>Status: <span class="tag">${product.stock}</span></p>
        <p>${product.description}</p>

        <div class="form-box" style="margin-top:1rem; padding:1rem;">
            <form method="post" action="/cart/add/${product.id}">
                <label for="qty">Quantity</label>
                <input id="qty" name="quantity" type="number" min="1" value="1" />
                <div class="actions">
                    <button class="btn" type="submit">Add to Cart</button>
                    <a class="btn secondary" href="/products">Back to Products</a>
                </div>
            </form>
        </div>
    </div>
</div>
</body>
</html>
""".trimIndent()

fun cartHtml(session: UserSession): String {
    val cart = getCart(session.username)

    val rows = if (cart.isEmpty()) {
        """
        <tr>
            <td colspan="5">Your cart is empty.</td>
        </tr>
        """
    } else {
        cart.joinToString("") { line ->
            val product = findProduct(line.productId) ?: return@joinToString ""
            val subtotal = product.price * line.quantity

            """
            <tr>
                <td>${product.name}</td>
                <td>
                    <form method="post" action="/cart/update/${product.id}" style="display:flex; gap:0.5rem; align-items:center;">
                        <input name="quantity" type="number" min="1" value="${line.quantity}" style="width:80px; margin:0;" />
                        <button type="submit">Update</button>
                    </form>
                </td>
                <td>£${"%.2f".format(product.price)}</td>
                <td>£${"%.2f".format(subtotal)}</td>
                <td>
                    <form method="post" action="/cart/remove/${product.id}">
                        <button type="submit">Remove</button>
                    </form>
                </td>
            </tr>
            """
        }
    }

    val total = cart.sumOf { line ->
        val product = findProduct(line.productId) ?: return@sumOf 0.0
        product.price * line.quantity
    }

    return """
<!DOCTYPE html>
<html>
<head>
    <title>Cart</title>
    ${commonStyles()}
</head>
<body>
${nav(session, "cart")}
<div class="container">
    <h1>My Cart</h1>
    <table>
        <thead>
            <tr>
                <th>Product</th>
                <th>Quantity</th>
                <th>Unit Price</th>
                <th>Subtotal</th>
                <th>Action</th>
            </tr>
        </thead>
        <tbody>
            $rows
        </tbody>
    </table>

    <div class="summary-box">
        <h3>Total: £${"%.2f".format(total)}</h3>
        <div class="actions">
            <form method="post" action="/checkout" style="display:inline;">
                <button class="btn" type="submit" ${if (cart.isEmpty()) "disabled" else ""}>Proceed to Checkout</button>
            </form>
            <a class="btn secondary" href="/products">Continue Shopping</a>
        </div>
    </div>
</div>
</body>
</html>
""".trimIndent()
}

fun checkoutHtml(session: UserSession) = """
<!DOCTYPE html>
<html>
<head>
    <title>Checkout</title>
    ${commonStyles()}
</head>
<body>
${nav(session, "checkout")}
<div class="container">
    <h1>Checkout</h1>
    <div class="form-box">
        <label>Delivery Address</label>
        <input type="text" placeholder="Enter your address" />

        <label>Delivery Time</label>
        <select>
            <option>09:00 - 11:00</option>
            <option>12:00 - 14:00</option>
            <option>15:00 - 17:00</option>
        </select>

        <label>Payment Method</label>
        <select>
            <option>Credit / Debit Card</option>
            <option>PayPal</option>
            <option>Cash on Delivery</option>
        </select>

        <div class="actions">
            <form method="post" action="/checkout" style="display:inline;">
                <button class="btn" type="submit">Place Order</button>
            </form>
            <a class="btn secondary" href="/cart">Back to Cart</a>
        </div>
    </div>
</div>
</body>
</html>
""".trimIndent()

fun ordersHtml(session: UserSession): String {
    val orders = getOrders(session.username)

    val rows = if (orders.isEmpty()) {
        """
        <tr>
            <td colspan="4">No orders yet.</td>
        </tr>
        """
    } else {
        orders.joinToString("") { order ->
            """
            <tr>
                <td>${order.orderId}</td>
                <td>${order.date}</td>
                <td>${order.status}</td>
                <td>£${"%.2f".format(order.total)}</td>
            </tr>
            """
        }
    }

    return """
<!DOCTYPE html>
<html>
<head>
    <title>Orders</title>
    ${commonStyles()}
</head>
<body>
${nav(session, "orders")}
<div class="container">
    <h1>My Orders</h1>
    <table>
        <thead>
            <tr>
                <th>Order ID</th>
                <th>Date</th>
                <th>Status</th>
                <th>Total</th>
            </tr>
        </thead>
        <tbody>
            $rows
        </tbody>
    </table>
</div>
</body>
</html>
""".trimIndent()
}

fun inventoryHtml(session: UserSession) = """
<!DOCTYPE html>
<html>
<head>
    <title>Inventory</title>
    ${commonStyles()}
</head>
<body>
${nav(session, "inventory")}
<div class="container">
    <h1>Inventory</h1>
    <p class="muted">Stock management page for employees and admins.</p>

    <table>
        <thead>
            <tr>
                <th>Product</th>
                <th>Category</th>
                <th>Current Stock</th>
                <th>Update Stock</th>
            </tr>
        </thead>
        <tbody>
            ${allProducts().joinToString("") { product ->
                """
                <tr>
                    <td>${product.name}</td>
                    <td>${product.category}</td>
                    <td>${product.stock}</td>
                    <td>
                        <form method="post" action="/inventory/update/${product.id}" style="display:flex; gap:0.5rem; align-items:center;">
                            <select name="stock" style="width:160px; margin:0;">
                                <option value="In stock" ${if (product.stock == "In stock") "selected" else ""}>In stock</option>
                                <option value="Low stock" ${if (product.stock == "Low stock") "selected" else ""}>Low stock</option>
                                <option value="Out of stock" ${if (product.stock == "Out of stock") "selected" else ""}>Out of stock</option>
                            </select>
                            <button type="submit">Update</button>
                        </form>
                    </td>
                </tr>
                """
            }}
        </tbody>
    </table>
</div>
</body>
</html>
""".trimIndent()

fun adminHtml(session: UserSession) = """
<!DOCTYPE html>
<html>
<head>
    <title>Admin Panel</title>
    ${commonStyles()}
</head>
<body>
${nav(session, "admin")}
<div class="container">
    <h1>Admin Dashboard</h1>
    <p class="muted">Overview of supermarket performance and stock status.</p>

    <div class="card-grid">
        <div class="card">
            <h3>Total Orders</h3>
            <p><strong>${totalOrdersCount()}</strong></p>
            <p class="muted">Number of orders placed by all users.</p>
        </div>

        <div class="card">
            <h3>Total Sales</h3>
            <p><strong>£${"%.2f".format(totalSalesAmount())}</strong></p>
            <p class="muted">Total value of all completed orders.</p>
        </div>

        <div class="card">
            <h3>Products</h3>
            <p><strong>${totalProductsCount()}</strong></p>
            <p class="muted">Total number of products in the catalogue.</p>
        </div>

        <div class="card">
            <h3>Low Stock Items</h3>
            <p><strong>${lowStockCount()}</strong></p>
            <p class="muted">Products currently marked as low stock.</p>
        </div>
        <div class="card">
            <h3>Add Product</h3>
            <p class="muted">Add a new product to the catalogue.</p>
            <a class="btn" href="/admin/products/add">Add Product</a>
        </div>
    </div>

    <div class="card" style="margin-top: 1.5rem;">
        <h2>Inventory Summary</h2>
        <table>
            <thead>
                <tr>
                    <th>Status</th>
                    <th>Count</th>
                </tr>
            </thead>
            <tbody>
                <tr>
                    <td>In stock</td>
                    <td>${totalProductsCount() - lowStockCount() - outOfStockCount()}</td>
                </tr>
                <tr>
                    <td>Low stock</td>
                    <td>${lowStockCount()}</td>
                </tr>
                <tr>
                    <td>Out of stock</td>
                    <td>${outOfStockCount()}</td>
                </tr>
            </tbody>
        </table>
    </div>
</div>
</body>
</html>
""".trimIndent()

fun adminUsersHtml(session: UserSession, userList: String) = """
<!DOCTYPE html>
<html>
<head>
    <title>Users</title>
    ${commonStyles()}
</head>
<body>
${nav(session, "users")}
<div class="container">
    <h1>Users</h1>
    <table>
        <thead>
            <tr>
                <th>Username</th>
                <th>Role</th>
            </tr>
        </thead>
        <tbody>
            $userList
        </tbody>
    </table>
</div>
</body>
</html>
""".trimIndent()

fun forbiddenHtml(session: UserSession) = """
<!DOCTYPE html>
<html>
<head>
    <title>Access Denied</title>
    ${commonStyles()}
</head>
<body>
${nav(session)}
<div class="container">
    <div class="card">
        <h1>403 — Access Denied</h1>
        <p>You do not have permission to view this page.</p>
    </div>
</div>
</body>
</html>
""".trimIndent()

fun notFoundHtml(session: UserSession, message: String) = """
<!DOCTYPE html>
<html>
<head>
    <title>Not Found</title>
    ${commonStyles()}
</head>
<body>
${nav(session)}
<div class="container">
    <div class="card">
        <h1>404 — Not Found</h1>
        <p>$message</p>
        <a class="btn secondary" href="/products">Back to Products</a>
    </div>
</div>
</body>
</html>
""".trimIndent()

fun analyticsHtml(
    session: UserSession,
    bestSellers: List<com.supermarket.repositories.ProductSales>,
    categorySales: List<com.supermarket.repositories.CategorySales>,
    lowStock: List<Triple<String, String, Int>>
): String {
    val bestSellersRows = if (bestSellers.isEmpty()) {
        "<tr><td colspan='4'>No sales data yet — place some orders first.</td></tr>"
    } else {
        bestSellers.joinToString("") { p ->
            """
            <tr>
                <td>${p.name}</td>
                <td>${p.category}</td>
                <td>${p.unitsSold}</td>
                <td>£${"%.2f".format(p.revenue)}</td>
            </tr>
            """
        }
    }

    val categoryRows = if (categorySales.isEmpty()) {
        "<tr><td colspan='3'>No sales data yet.</td></tr>"
    } else {
        categorySales.joinToString("") { c ->
            """
            <tr>
                <td>${c.category}</td>
                <td>${c.unitsSold}</td>
                <td>£${"%.2f".format(c.revenue)}</td>
            </tr>
            """
        }
    }

    val lowStockRows = if (lowStock.isEmpty()) {
        "<tr><td colspan='3'>All products have healthy stock levels.</td></tr>"
    } else {
        lowStock.joinToString("") { (name, sku, qty) ->
            val color = if (qty == 0) "color:red;" else "color:orange;"
            """
            <tr>
                <td>$name</td>
                <td>$sku</td>
                <td style="$color"><strong>$qty</strong></td>
            </tr>
            """
        }
    }

    return """
<!DOCTYPE html>
<html>
<head>
    <title>Analytics</title>
    ${commonStyles()}
</head>
<body>
${nav(session, "analytics")}
<div class="container">
    <h1>Management Analytics</h1>
    <p class="muted">Sales performance and stock overview.</p>

    <h2>Best Selling Products</h2>
    <table>
        <thead>
            <tr>
                <th>Product</th>
                <th>Category</th>
                <th>Units Sold</th>
                <th>Revenue</th>
            </tr>
        </thead>
        <tbody>
            $bestSellersRows
        </tbody>
    </table>

    <h2 style="margin-top:2rem;">Sales by Category</h2>
    <table>
        <thead>
            <tr>
                <th>Category</th>
                <th>Units Sold</th>
                <th>Revenue</th>
            </tr>
        </thead>
        <tbody>
            $categoryRows
        </tbody>
    </table>

    <h2 style="margin-top:2rem;">Low Stock Alert</h2>
    <table>
        <thead>
            <tr>
                <th>Product</th>
                <th>SKU</th>
                <th>Quantity Available</th>
            </tr>
        </thead>
        <tbody>
            $lowStockRows
        </tbody>
    </table>
</div>
</body>
</html>
""".trimIndent()
}

fun addProductHtml(session: UserSession, error: String = "", success: String = "") = """
<!DOCTYPE html>
<html>
<head>
    <title>Add Product</title>
    ${commonStyles()}
</head>
<body>
${nav(session, "admin")}
<div class="container">
    <h1>Add New Product</h1>
    <p class="muted">Fill in the details below to add a product to the catalogue.</p>

    ${if (success.isNotEmpty()) "<div style='background:#e8f5e9;color:#1b5e20;padding:1rem;border-radius:8px;margin-bottom:1rem;'>$success</div>" else ""}
    ${if (error.isNotEmpty()) "<div style='background:#ffebee;color:#b71c1c;padding:1rem;border-radius:8px;margin-bottom:1rem;'>$error</div>" else ""}

    <div class="form-box">
        <form method="post" action="/admin/products/add">
            <label>Product Name</label>
            <input type="text" name="name" placeholder="e.g. Organic Apples" required />

            <label>Description</label>
            <input type="text" name="description" placeholder="e.g. Fresh organic apples from local farms" />

            <label>Category</label>
            <select name="category">
                <option value="fruit-vegetables">Fruit & Vegetables</option>
                <option value="dairy-eggs">Dairy & Eggs</option>
                <option value="bakery">Bakery</option>
                <option value="meat-fish">Meat & Fish</option>
                <option value="drinks">Drinks</option>
            </select>

            <label>Price (£)</label>
            <input type="number" name="price" step="0.01" min="0" placeholder="e.g. 1.99" required />

            <label>SKU</label>
            <input type="text" name="sku" placeholder="e.g. SKU-APL-002" required />

            <label>Initial Stock Quantity</label>
            <input type="number" name="stock" min="0" value="100" required />

            <div class="actions">
                <button class="btn" type="submit">Add Product</button>
                <a class="btn secondary" href="/admin">Back to Admin</a>
            </div>
        </form>
    </div>
</div>
</body>
</html>
""".trimIndent()

fun registerPageHtml(error: String = "", success: String = "") = """
<!DOCTYPE html>
<html>
<head>
    <title>Register</title>
    <style>
        body {
            font-family: Arial, sans-serif;
            display:flex;
            justify-content:center;
            align-items:center;
            height:100vh;
            margin:0;
            background:#f0f4f8;
        }
        .box {
            background:white;
            padding:2rem;
            border-radius:12px;
            box-shadow:0 2px 10px rgba(0,0,0,.15);
            width:320px;
        }
        h1 { margin-top:0; }
        input {
            width:100%;
            padding:.7rem;
            margin:.4rem 0 1rem;
            box-sizing:border-box;
            border:1px solid #ccc;
            border-radius:8px;
        }
        button {
            width:100%;
            padding:.8rem;
            background:#2e7d32;
            color:white;
            border:none;
            border-radius:8px;
            cursor:pointer;
            font-size:1rem;
        }
        .error   { margin-top:1rem; color:red;   font-size:.9rem; }
        .success { margin-top:1rem; color:green; font-size:.9rem; }
        .login-link { margin-top:1rem; text-align:center; font-size:.9rem; }
        a { color:#2e7d32; }
    </style>
</head>
<body>
<div class="box">
    <h1>Create Account</h1>
    <form method="post" action="/register">
        <input type="text"     name="username"  placeholder="Username"         required />
        <input type="password" name="password"  placeholder="Password"         required />
        <input type="password" name="password2" placeholder="Confirm Password" required />
        <button type="submit">Register</button>
    </form>
    ${if (error.isNotEmpty())   "<p class=\"error\">$error</p>"     else ""}
    ${if (success.isNotEmpty()) "<p class=\"success\">$success</p>" else ""}
    <p class="login-link">Already have an account? <a href="/login">Login</a></p>
</div>
</body>
</html>
""".trimIndent()

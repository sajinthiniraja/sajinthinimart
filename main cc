#include <drogon/drogon.h>
#include <sqlite3.h>
#include <filesystem>
#include <mutex>
#include <string>

using namespace drogon;
static sqlite3 *db = nullptr;
static std::mutex dbMutex;

static void sql(const std::string &q) {
    char *err = nullptr;
    if (sqlite3_exec(db, q.c_str(), nullptr, nullptr, &err) != SQLITE_OK) {
        LOG_ERROR << "SQLite: " << (err ? err : "unknown");
        sqlite3_free(err);
    }
}
static std::string esc(const std::string &s) {
    std::string o;
    for (char c : s) { if (c == '\'') o += "''"; else o += c; }
    return o;
}
static Json::Value rows(const std::string &q) {
    Json::Value out(Json::arrayValue);
    sqlite3_stmt *st = nullptr;
    if (sqlite3_prepare_v2(db, q.c_str(), -1, &st, nullptr) == SQLITE_OK) {
        int n = sqlite3_column_count(st);
        while (sqlite3_step(st) == SQLITE_ROW) {
            Json::Value r(Json::objectValue);
            for (int i=0;i<n;i++) {
                const char *name = sqlite3_column_name(st,i);
                const unsigned char *v = sqlite3_column_text(st,i);
                r[name] = v ? reinterpret_cast<const char*>(v) : "";
            }
            out.append(r);
        }
    }
    sqlite3_finalize(st);
    return out;
}
static Json::Value bodyJson(const HttpRequestPtr &req) {
    auto b = req->getJsonObject();
    return b ? *b : Json::Value(Json::objectValue);
}
static HttpResponsePtr json(const Json::Value &v, HttpStatusCode code = k200OK) {
    auto r = HttpResponse::newHttpJsonResponse(v);
    r->setStatusCode(code);
    return r;
}
static Json::Value message(const std::string &s) { Json::Value v; v["message"]=s; return v; }

int main() {
    std::filesystem::create_directories("data");
    if (sqlite3_open("data/yashimart.db", &db) != SQLITE_OK) {
        std::cerr << "Could not open SQLite database\n"; return 1;
    }
    sql("PRAGMA foreign_keys=ON;");
    sql("CREATE TABLE IF NOT EXISTS users(id INTEGER PRIMARY KEY AUTOINCREMENT,name TEXT NOT NULL,email TEXT UNIQUE NOT NULL,password TEXT NOT NULL,role TEXT NOT NULL CHECK(role IN ('buyer','seller','admin')));");
    sql("CREATE TABLE IF NOT EXISTS products(id INTEGER PRIMARY KEY AUTOINCREMENT,seller_id INTEGER DEFAULT 0,name TEXT NOT NULL,category TEXT NOT NULL,price REAL NOT NULL,image TEXT NOT NULL,description TEXT NOT NULL DEFAULT '',stock INTEGER NOT NULL DEFAULT 20,FOREIGN KEY(seller_id) REFERENCES users(id));");
    sql("CREATE TABLE IF NOT EXISTS orders(id INTEGER PRIMARY KEY AUTOINCREMENT,buyer_email TEXT NOT NULL,items TEXT NOT NULL,total REAL NOT NULL,status TEXT NOT NULL DEFAULT 'Pending',created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP);");
    sql("CREATE TABLE IF NOT EXISTS reviews(id INTEGER PRIMARY KEY AUTOINCREMENT,product_id INTEGER NOT NULL,user_name TEXT NOT NULL,rating INTEGER NOT NULL,comment TEXT NOT NULL);");
    auto existing = rows("SELECT id FROM products LIMIT 1");
    if (existing.empty()) {
        sql("INSERT INTO products(name,category,price,image,description,stock) VALUES"
            "('Wireless Headphones','Electronics',1499,'https://images.unsplash.com/photo-1505740420928-5e560c06d30e?auto=format&fit=crop&w=700&q=80','Comfortable wireless sound for everyday use',30),"
            "('Smart Watch','Electronics',2299,'https://images.unsplash.com/photo-1523275335684-37898b6baf30?auto=format&fit=crop&w=700&q=80','Track your day in style',20),"
            "('Everyday Backpack','Fashion',899,'https://images.unsplash.com/photo-1553062407-98eeb64c6a62?auto=format&fit=crop&w=700&q=80','Lightweight backpack for college and travel',25),"
            "('Running Shoes','Fashion',1799,'https://images.unsplash.com/photo-1542291026-7eec264c27ff?auto=format&fit=crop&w=700&q=80','Everyday comfort and grip',18),"
            "('Coffee Maker','Home',2499,'https://images.unsplash.com/photo-1495474472287-4d71bcdd2085?auto=format&fit=crop&w=700&q=80','Brew your favourite coffee at home',10),"
            "('Desk Lamp','Home',699,'https://images.unsplash.com/photo-150 lamp','Modern desk lamp',12);");
        // Replace intentionally invalid seed image URL with a valid one.
        sql("DELETE FROM products WHERE image='https://images.unsplash.com/photo-150 lamp';");
        sql("INSERT INTO products(name,category,price,image,description,stock) VALUES('Desk Lamp','Home',699,'https://images.unsplash.com/photo-1507473885765-e6ed057f782c?auto=format&fit=crop&w=700&q=80','Modern lamp for your study table',12);");
    }

    app().registerHandler("/api/health", [](const HttpRequestPtr&, std::function<void(const HttpResponsePtr&)>&& cb) {
        Json::Value v; v["status"]="ok"; v["database"]="SQLite connected"; cb(json(v));
    }, {Get});

    app().registerHandler("/api/products", [](const HttpRequestPtr& req, std::function<void(const HttpResponsePtr&)>&& cb) {
        std::lock_guard<std::mutex> lock(dbMutex);
        std::string q = "SELECT id,seller_id,name,category,price,image,description,stock FROM products WHERE 1=1";
        auto term = req->getParameter("q"), cat = req->getParameter("category");
        if (!term.empty()) q += " AND (name LIKE '%" + esc(term) + "%' OR description LIKE '%" + esc(term) + "%')";
        if (!cat.empty() && cat != "All") q += " AND category='" + esc(cat) + "'";
        q += " ORDER BY id DESC";
        cb(json(rows(q)));
    }, {Get});

    app().registerHandler("/api/register", [](const HttpRequestPtr& req, std::function<void(const HttpResponsePtr&)>&& cb) {
        auto b=bodyJson(req); std::string name=b.get("name","").asString(), email=b.get("email","").asString(), pass=b.get("password","").asString(), role=b.get("role","buyer").asString();
        if(name.empty()||email.empty()||pass.size()<4||(role!="buyer"&&role!="seller")) { cb(json(message("Enter name, email, password (4+ characters) and choose buyer or seller."), k400BadRequest)); return; }
        std::lock_guard<std::mutex> lock(dbMutex);
        sql("INSERT INTO users(name,email,password,role) VALUES('"+esc(name)+"','"+esc(email)+"','"+esc(pass)+"','"+role+"')");
        if(sqlite3_changes(db)==0) { cb(json(message("Registration failed. This email may already be registered."), k400BadRequest)); return; }
        Json::Value v; v["message"]="Registration successful"; v["name"]=name; v["email"]=email; v["role"]=role; cb(json(v));
    }, {Post});

    app().registerHandler("/api/login", [](const HttpRequestPtr& req, std::function<void(const HttpResponsePtr&)>&& cb) {
        auto b=bodyJson(req); std::string email=b.get("email","").asString(), pass=b.get("password","").asString(), role=b.get("role","buyer").asString();
        std::lock_guard<std::mutex> lock(dbMutex);
        if(role=="admin" && email=="admin@yashimart.local" && pass=="admin123") {
            Json::Value v; v["message"]="Admin login successful"; v["name"]="Admin"; v["email"]=email; v["role"]="admin"; cb(json(v)); return;
        }
        auto a=rows("SELECT name,email,role FROM users WHERE email='"+esc(email)+"' AND password='"+esc(pass)+"' AND role='"+esc(role)+"' LIMIT 1");
        if(a.empty()) { cb(json(message("Login failed. Check your details and role."), k401Unauthorized)); return; }
        auto v=a[0]; v["message"]="Login successful"; cb(json(v));
    }, {Post});

    app().registerHandler("/api/products", [](const HttpRequestPtr& req, std::function<void(const HttpResponsePtr&)>&& cb) {
        auto b=bodyJson(req); std::string name=b.get("name","").asString(), cat=b.get("category","Other").asString(), image=b.get("image","").asString(), desc=b.get("description","").asString();
        double price=b.get("price",0).asDouble(); int stock=b.get("stock",1).asInt(); std::string seller=b.get("seller_id","0").asString();
        if(name.empty()||image.empty()||price<=0) { cb(json(message("Product name, image URL and positive price are required."), k400BadRequest)); return; }
        std::lock_guard<std::mutex> lock(dbMutex);
        sql("INSERT INTO products(seller_id,name,category,price,image,description,stock) VALUES("+esc(seller)+",'"+esc(name)+"','"+esc(cat)+"',"+std::to_string(price)+",'"+esc(image)+"','"+esc(desc)+"',"+std::to_string(stock)+")");
        Json::Value v=message("Product added"); v["id"]=static_cast<Json::Int64>(sqlite3_last_insert_rowid(db)); cb(json(v));
    }, {Post});

    app().registerHandler("/api/products/{1}", [](const HttpRequestPtr& req, std::function<void(const HttpResponsePtr&)>&& cb, const std::string& id) {
        std::lock_guard<std::mutex> lock(dbMutex);
        sql("DELETE FROM products WHERE id="+esc(id));
        cb(json(message("Product deleted")));
    }, {Delete});

    app().registerHandler("/api/orders", [](const HttpRequestPtr& req, std::function<void(const HttpResponsePtr&)>&& cb) {
        auto b=bodyJson(req); std::string email=b.get("email","").asString(), items=b.get("items","").asString(); double total=b.get("total",0).asDouble();
        if(email.empty()||items.empty()||total<=0) { cb(json(message("Cart is empty or user email is missing."), k400BadRequest)); return; }
        std::lock_guard<std::mutex> lock(dbMutex);
        sql("INSERT INTO orders(buyer_email,items,total,status) VALUES('"+esc(email)+"','"+esc(items)+"',"+std::to_string(total)+",'Pending')");
        Json::Value v=message("Order placed successfully"); v["order_id"]=static_cast<Json::Int64>(sqlite3_last_insert_rowid(db)); cb(json(v));
    }, {Post});

    app().registerHandler("/api/orders", [](const HttpRequestPtr& req, std::function<void(const HttpResponsePtr&)>&& cb) {
        std::lock_guard<std::mutex> lock(dbMutex);
        std::string email=req->getParameter("email");
        std::string q="SELECT id,buyer_email,items,total,status,created_at FROM orders";
        if(!email.empty()) q += " WHERE buyer_email='"+esc(email)+"'";
        q += " ORDER BY id DESC";
        cb(json(rows(q)));
    }, {Get});

    app().registerHandler("/api/reviews", [](const HttpRequestPtr& req, std::function<void(const HttpResponsePtr&)>&& cb) {
        std::lock_guard<std::mutex> lock(dbMutex);
        auto p=req->getParameter("product_id");
        cb(json(rows("SELECT id,product_id,user_name,rating,comment FROM reviews"+(p.empty()?std::string(""):" WHERE product_id="+esc(p))+" ORDER BY id DESC")));
    }, {Get});
    app().registerHandler("/api/reviews", [](const HttpRequestPtr& req, std::function<void(const HttpResponsePtr&)>&& cb) {
        auto b=bodyJson(req); int pid=b.get("product_id",0).asInt(), rating=b.get("rating",5).asInt();
        std::string name=b.get("user_name","Guest").asString(), comment=b.get("comment","").asString();
        if(pid<=0||rating<1||rating>5||comment.empty()) { cb(json(message("Product, rating (1-5) and comment are required."), k400BadRequest)); return; }
        std::lock_guard<std::mutex> lock(dbMutex);
        sql("INSERT INTO reviews(product_id,user_name,rating,comment) VALUES("+std::to_string(pid)+",'"+esc(name)+"',"+std::to_string(rating)+",'"+esc(comment)+"')");
        cb(json(message("Review saved")));
    }, {Post});

    app().registerHandler("/api/admin/users", [](const HttpRequestPtr&, std::function<void(const HttpResponsePtr&)>&& cb) {
        std::lock_guard<std::mutex> lock(dbMutex);
        cb(json(rows("SELECT id,name,email,role FROM users ORDER BY id DESC")));
    }, {Get});
    app().registerHandler("/api/admin/orders", [](const HttpRequestPtr&, std::function<void(const HttpResponsePtr&)>&& cb) {
        std::lock_guard<std::mutex> lock(dbMutex);
        cb(json(rows("SELECT id,buyer_email,items,total,status,created_at FROM orders ORDER BY id DESC")));
    }, {Get});

    app().addListener("0.0.0.0", 8080);
    app().setDocumentRoot("./public");
    app().registerHandler("/", [](const HttpRequestPtr&, std::function<void(const HttpResponsePtr&)>&& cb) {
        auto r=HttpResponse::newFileResponse("./public/index.html"); cb(r);
    }, {Get});
    LOG_INFO << "YashiMart starting on http://localhost:8080";
    app().run();
    sqlite3_close(db);
}

import { categoryEnum } from "./db/schema";

type Rule = {
  category: string;
  keywords: string[];
};

function escapeRegex(s: string): string {
  return s.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
}

const RULES: Rule[] = [
  {
    category: "Income",
    keywords: [
      "salary", "payroll", "monthly pay", "wage",
      "bonus", "performance bonus", "incentive bonus",
      "commission", "sales commission",
      "freelance", "contract payment", "gig payment",
      "business income", "sales proceeds", "merchant settlement",
      "rent received", "rental income", "tenant payment",
      "investment return", "capital gains", "broker credit",
      "dividend", "stock dividend", "fund payout",
      "interest credit", "savings interest", "fixed deposit interest",
      "cashback", "reward cashback", "cash back",
      "refund", "merchant refund", "reversal credit",
      "reimbursement", "expense claim", "claims payment",
      "transfer from", "received from", "inward transfer",
      "gift money", "received gift",
      "grant", "stipend", "allowance payment",
      "tax refund", "irs refund", "revenue refund",
      "pension", "retirement payout", "annuity",
      "child support received", "support payment in",
      "alimony received", "spousal support in",
      "payment received", "credit alert", "inflow",
    ],
  },
  {
    category: "Food & Dining",
    keywords: [
      "grocery", "supermarket", "shoprite", "spar", "market",
      "restaurant", "eatery", "dining", "bistro",
      "kfc", "mcdonalds", "burger", "pizza", "dominos", "subway",
      "cafe", "coffee", "coffee shop", "starbucks",
      "bakery", "bread", "pastry", "cake shop",
      "snack", "confectionery", "treats",
      "food delivery", "glovo", "ubereats", "bolt food",
      "meal plan", "meal subscription", "meal prep",
      "beverage", "juice", "smoothie", "soda", "drink", "drinks",
      "bottled water", "water dispenser", "sachet water",
    ],
  },
  {
    category: "Transport",
    keywords: [
      "uber", "bolt", "taxify", "ride",
      "taxi", "cab fare",
      "bus", "brt", "coach",
      "train", "rail", "metro",
      "flight", "airline", "airways", "ticket airfare",
      "fuel", "petrol", "diesel", "filling station",
      "toll", "tollgate", "bridge toll",
      "parking", "park fee", "car park",
      "mechanic", "car service", "oil change", "alignment",
      "car wash", "auto wash",
      "spare parts", "vehicle parts", "tyre",
      "delivery", "dispatch", "courier transport",
      "logistics", "haulage", "freight",
      "okada", "bike ride", "motorbike",
      "ferry", "boat", "water transport",
    ],
  },
  {
    category: "Housing",
    keywords: [
      "rent", "house rent", "tenant rent",
      "mortgage", "home loan payment",
      "home repair", "plumbing", "electrician", "carpentry",
      "maintenance fee", "estate due", "facility fee",
      "furniture", "sofa", "table", "wardrobe",
      "appliance", "fridge", "washing machine", "microwave",
      "cleaning", "janitorial", "housekeeping",
      "security levy", "guard service", "cctv",
      "lawma", "waste", "trash", "garbage",
      "moving", "relocation", "packers", "movers",
    ],
  },
  {
    category: "Utilities",
    keywords: [
      "electricity", "nepa", "phcn", "disco", "ikedc", "eko disco",
      "water bill", "water corp", "water utility",
      "cooking gas", "gas refill", "lpg",
      "internet", "wifi", "broadband", "fiber",
      "airtime", "recharge", "topup",
      "data bundle", "data plan", "mobile data",
      "data mtn", "datamtn", "mtn data", "airtel data", "glo data", "9mobile data", "dataairtel", "dataglo",
      "dstv", "gotv", "startimes", "cable tv",
      "phone bill", "telecom bill", "postpaid",
      "sewage", "drainage", "sanitation",
      "generator fuel", "gen service", "inverter",
    ],
  },
  {
    category: "Healthcare",
    keywords: [
      "pharmacy", "drug store", "medication",
      "hospital", "clinic", "medical center",
      "doctor", "consultation", "physician",
      "dental", "dentist", "tooth",
      "optical", "eye clinic", "glasses",
      "lab test", "diagnostic", "scan", "xray",
      "therapy", "counseling", "psychologist",
      "maternity", "prenatal", "delivery ward",
      "health insurance", "hmo", "nhis",
      "wellness", "supplement", "vitamins",
    ],
  },
  {
    category: "Education",
    keywords: [
      "tuition", "school fees", "college fee",
      "bookstore", "textbook", "books",
      "course", "bootcamp", "online class",
      "exam fee", "test fee", "registration exam",
      "certification", "certificate exam", "professional exam",
      "stationery", "school supplies", "notebook",
      "school bus", "campus shuttle",
      "hostel", "dorm", "boarding",
      "daycare", "creche", "after school",
      "pta", "school levy", "school donation",
    ],
  },
  {
    category: "Shopping",
    keywords: [
      "clothes", "clothing", "fashion", "apparel",
      "shoes", "sneakers", "footwear",
      "bag", "handbag", "backpack",
      "electronics", "laptop", "phone", "tablet",
      "gadget", "accessory", "earbuds", "charger",
      "beauty", "cosmetics", "makeup", "skincare",
      "toiletries", "personal care", "hygiene",
      "home goods", "kitchenware", "decor",
      "baby shop", "kids store", "toy store",
      "gift shop", "gift item", "present", "gift",
    ],
  },
  {
    category: "Subscriptions",
    keywords: [
      "spotify", "apple music", "boomplay",
      "netflix", "prime video", "showmax",
      "icloud", "google one", "dropbox", "onedrive",
      "notion", "evernote", "todoist", "grammarly",
      "github", "gitlab", "vercel", "digitalocean",
      "newspaper subscription", "news subscription", "medium membership",
      "gym membership", "fitness app", "peloton",
      "coursera", "udemy", "pluralsight", "masterclass",
    ],
  },
  {
    category: "Entertainment",
    keywords: [
      "streaming service", "video on demand", "ott subscription",
      "cinema", "movie", "filmhouse", "silverbird",
      "event", "ticket", "concert", "show",
      "game", "playstation", "xbox", "steam",
      "sports", "match ticket", "stadium",
      "bar", "lounge", "club",
      "hobby", "craft", "musical instrument",
      "kindle", "audiobook", "book subscription",
      "tour", "sightseeing", "tourist",
      "park entry", "theme park", "recreation",
    ],
  },
  {
    category: "Finance",
    keywords: [
      "bank charge", "maintenance fee", "sms charge",
      "transfer fee", "transaction fee", "service charge",
      "transfer to", "sent to", "outward transfer", "trf to",
      "atm fee", "cash withdrawal fee",
      "loan repayment", "loan payment", "installment",
      "credit card payment", "card repayment",
      "savings deposit", "piggyvest", "cowrywise",
      "investment", "mutual fund", "treasury bill",
      "tax payment", "vat", "withholding tax", "irs payment",
      "penalty", "fine", "late fee",
      "fx", "forex", "usd purchase", "currency exchange",
    ],
  },
  {
    category: "Family",
    keywords: [
      "family support", "sent to mom", "sent to dad",
      "nanny", "babysitter", "child care",
      "house help", "domestic staff", "maid salary",
      "pet food", "vet", "pet care",
      "birthday", "wedding", "anniversary",
    ],
  },
  {
    category: "Travel",
    keywords: [
      "hotel", "lodging", "accommodation",
      "visa fee", "embassy fee",
      "passport", "immigration service",
      "travel insurance", "trip cover",
      "car rental", "rent a car",
    ],
  },
  {
    category: "Work",
    keywords: [
      "office supplies", "printer ink", "paper",
      "software subscription", "saas", "license fee",
      "aws", "azure", "gcp", "cloud hosting",
      "domain renewal", "hosting", "namecheap",
      "ads", "facebook ads", "google ads", "promotion",
    ],
  },
  {
    category: "Charity",
    keywords: [
      "donation", "charity", "ngo",
      "tithe", "offering", "church",
      "zakat", "sadaqah",
      "community levy", "union due",
      "relief fund", "support drive",
    ],
  },
  {
    category: "Insurance",
    keywords: [
      "life insurance", "life premium",
      "car insurance", "motor insurance", "comprehensive cover",
      "property insurance", "home insurance",
      "phone insurance", "device insurance",
      "business insurance", "liability cover",
    ],
  },
  {
    category: "Crypto",
    keywords: [
      "buy btc", "buy usdt", "crypto purchase",
      "sell btc", "sell usdt", "crypto sale",
      "onchain transfer", "wallet transfer", "blockchain fee",
      "trading fee", "maker fee", "taker fee",
      "staking reward", "validator reward",
    ],
  },
  {
    category: "Government",
    keywords: [
      "firs", "state tax", "paye", "land use charge",
      "license renewal", "permit fee", "business permit",
      "traffic fine", "frsc fine", "parking violation",
      "court fee", "filing fee",
      "immigration fee", "residence permit",
    ],
  },
  {
    category: "Legal",
    keywords: [
      "legal consultation", "attorney fee", "lawyer",
      "notary", "affidavit", "legal document",
      "legal filing", "registration filing",
      "legal settlement", "claim settlement",
      "compliance fee", "regulatory filing",
    ],
  },
  {
    category: "Business",
    keywords: [
      "staff salary", "employee payroll", "wages",
      "contractor payment", "vendor payout",
      "stock purchase", "inventory restock", "wholesale",
      "office electricity", "office internet", "office rent",
      "fleet fuel", "vehicle fleet", "transport ops",
      "pos charge", "merchant fee", "terminal fee",
      "packaging", "carton", "shipping material",
      "printing", "branding", "flyers", "business cards",
    ],
  },
  {
    category: "Personal",
    keywords: [
      "barbing", "haircut", "salon",
      "spa", "massage", "facial",
      "gym", "fitness center", "personal trainer",
      "laundry", "dry cleaning", "wash and fold",
      "tailor", "sewing", "alteration",
    ],
  },
  {
    category: "Communication",
    keywords: [
      "dhl", "fedex", "ups", "parcel",
      "post office", "postal service", "mail fee",
      "international call", "voice bundle",
      "sms bundle", "bulk sms",
      "roaming", "international roaming",
    ],
  },
  {
    category: "Events",
    keywords: [
      "wedding vendor", "bridal", "groom outfit",
      "birthday cake", "party decor", "party hall",
      "funeral", "burial", "memorial",
      "conference", "summit", "event registration",
      "crusade", "retreat", "pilgrimage",
    ],
  },
  {
    category: "Misc",
    keywords: [
      "card replacement", "stolen card", "fraud reversal",
      "chargeback", "dispute", "claim dispute",
      "misc expense", "other expense", "unknown debit",
      "misc income", "other income", "unknown credit",
      "self transfer", "own account transfer", "wallet topup",
    ],
  },
];

export type Category = (typeof RULES)[number]["category"] | "Other";

const COMPILED_RULES = RULES.map((rule) => ({
  category: rule.category as Category,
  pattern: new RegExp(`\\b(?:${rule.keywords.map(escapeRegex).join("|")})\\b`, "i"),
}));

const TRANSFER_IN_RE = /\b(?:transfer from|received from)\b/i;
const TRANSFER_OUT_RE = /\b(?:transfer to|sent to|outward transfer)\b/i;

export function classifyTransaction(
  description: string,
  receipient?: string,
  amount?: number,
): typeof categoryEnum.enumName {
  const text = `${description} ${receipient ?? ""}`.replace(/\s+/g, " ").trim();

  if (typeof amount === "number" && amount > 0 && TRANSFER_IN_RE.test(text)) {
    return "Income";
  }

  if (typeof amount === "number" && amount < 0 && TRANSFER_OUT_RE.test(text)) {
    return "Finance";
  }

  for (const rule of COMPILED_RULES) {
    if (rule.pattern.test(text)) {
      return rule.category;
    }
  }

  return "Other";
}

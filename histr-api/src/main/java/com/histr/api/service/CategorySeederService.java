package com.histr.api.service;

import com.histr.api.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CategorySeederService {

    private final CategoryRepository categoryRepository;
    private final ClassifierService classifierService;

    record CategoryDef(String label, String embeddingText) {}

    private static final List<CategoryDef> CATEGORIES = List.of(
        new CategoryDef("Income", "salary payroll monthly pay wage bonus performance bonus incentive bonus commission sales commission freelance contract payment gig payment business income sales proceeds merchant settlement rent received rental income tenant payment investment return capital gains broker credit dividend stock dividend fund payout interest credit savings interest fixed deposit interest cashback reward cashback cash back refund merchant refund reversal credit reimbursement expense claim claims payment transfer from received from inward transfer gift money received gift grant stipend allowance payment tax refund irs refund revenue refund pension retirement payout annuity child support received support payment in alimony received spousal support in payment received credit alert inflow"),
        new CategoryDef("Food & Dining", "grocery supermarket shoprite spar market restaurant eatery dining bistro kfc mcdonalds burger pizza dominos subway cafe coffee coffee shop starbucks bakery bread pastry cake shop snack confectionery treats food delivery glovo ubereats bolt food meal plan meal subscription meal prep beverage juice smoothie soda drink drinks bottled water water dispenser sachet water"),
        new CategoryDef("Transport", "uber bolt taxify ride taxi cab fare bus brt coach train rail metro flight airline airways ticket airfare fuel petrol diesel filling station toll tollgate bridge toll parking park fee car park mechanic car service oil change alignment car wash auto wash spare parts vehicle parts tyre delivery dispatch courier transport logistics haulage freight okada bike ride motorbike ferry boat water transport"),
        new CategoryDef("Housing", "rent house rent tenant rent mortgage home loan payment home repair plumbing electrician carpentry maintenance fee estate due facility fee furniture sofa table wardrobe appliance fridge washing machine microwave cleaning janitorial housekeeping security levy guard service cctv lawma waste trash garbage moving relocation packers movers"),
        new CategoryDef("Utilities", "electricity nepa phcn disco ikedc eko disco water bill water corp water utility cooking gas gas refill lpg internet wifi broadband fiber dstv gotv startimes cable tv phone bill telecom bill postpaid sewage drainage sanitation generator fuel gen service inverter"),
        new CategoryDef("Healthcare", "pharmacy drug store medication hospital clinic medical center doctor consultation physician dental dentist tooth optical eye clinic glasses lab test diagnostic scan xray therapy counseling psychologist maternity prenatal delivery ward health insurance hmo nhis wellness supplement vitamins"),
        new CategoryDef("Education", "tuition school fees college fee bookstore textbook books course bootcamp online class exam fee test fee registration exam certification certificate exam professional exam stationery school supplies notebook school bus campus shuttle hostel dorm boarding daycare creche after school pta school levy school donation"),
        new CategoryDef("Shopping", "clothes clothing fashion apparel shoes sneakers footwear bag handbag backpack electronics laptop phone tablet gadget accessory earbuds charger beauty cosmetics makeup skincare toiletries personal care hygiene home goods kitchenware decor baby shop kids store toy store gift shop gift item present gift"),
        new CategoryDef("Subscriptions", "spotify apple music boomplay netflix prime video showmax icloud google one dropbox onedrive notion evernote todoist grammarly github gitlab vercel digitalocean newspaper subscription news subscription medium membership gym membership fitness app peloton coursera udemy pluralsight masterclass"),
        new CategoryDef("Entertainment", "streaming service video on demand ott subscription cinema movie filmhouse silverbird event ticket concert show game playstation xbox steam sports match ticket stadium bar lounge club hobby craft musical instrument kindle audiobook book subscription tour sightseeing tourist park entry theme park recreation"),
        new CategoryDef("Finance", "bank charge maintenance fee sms charge transfer fee transaction fee service charge transfer to sent to outward transfer trf to atm fee cash withdrawal fee loan repayment loan payment installment credit card payment card repayment savings deposit piggyvest cowrywise investment mutual fund treasury bill tax payment vat withholding tax irs payment penalty fine late fee fx forex usd purchase currency exchange"),
        new CategoryDef("Family", "family support sent to mom sent to dad nanny babysitter child care house help domestic staff maid salary pet food vet pet care birthday wedding anniversary"),
        new CategoryDef("Travel", "hotel lodging accommodation visa fee embassy fee passport immigration service travel insurance trip cover car rental rent a car"),
        new CategoryDef("Work", "office supplies printer ink paper software subscription saas license fee aws azure gcp cloud hosting domain renewal hosting namecheap ads facebook ads google ads promotion"),
        new CategoryDef("Charity", "donation charity ngo tithe offering church zakat sadaqah community levy union due relief fund support drive"),
        new CategoryDef("Insurance", "life insurance life premium car insurance motor insurance comprehensive cover property insurance home insurance phone insurance device insurance business insurance liability cover"),
        new CategoryDef("Government", "firs state tax paye land use charge license renewal permit fee business permit traffic fine frsc fine parking violation court fee filing fee immigration fee residence permit"),
        new CategoryDef("Legal", "legal consultation attorney fee lawyer notary affidavit legal document legal filing registration filing legal settlement claim settlement compliance fee regulatory filing"),
        new CategoryDef("Business", "staff salary employee payroll wages contractor payment vendor payout stock purchase inventory restock wholesale office electricity office internet office rent fleet fuel vehicle fleet transport ops pos charge merchant fee terminal fee packaging carton shipping material printing branding flyers business cards"),
        new CategoryDef("Personal", "barbing haircut salon spa massage facial gym fitness center personal trainer laundry dry cleaning wash and fold tailor sewing alteration"),
        new CategoryDef("Communication", "dhl fedex ups parcel post office postal service mail fee international call voice bundle sms bundle bulk sms roaming international roaming"),
        new CategoryDef("Events", "wedding vendor bridal groom outfit birthday cake party decor party hall funeral burial memorial conference summit event registration crusade retreat pilgrimage"),
        new CategoryDef("Misc", "card replacement stolen card fraud reversal chargeback dispute claim dispute misc expense other expense unknown debit misc income other income unknown credit self transfer own account transfer wallet topup"),
        new CategoryDef("Mobile Services", "airtime recharge topup voice call credit phone credit mobile credit call credit airtime purchase airtime topup mtn airtime airtel airtime glo airtime 9mobile airtime airtime bundle airtime transfer data bundle data plan mobile data mtn data airtel data glo data 9mobile data data subscription internet data mobile internet data recharge data topup data purchase data bundle activation data rollover data plan renewal broadband data cellular data 4g data 5g data")
    );

    public void seed() {
        if (categoryRepository.count() > 0) {
            log.info("Categories already seeded");
            return;
        }

        log.info("Seeding categories...");
        for (CategoryDef cat : CATEGORIES) {
            float[] embedding = classifierService.embed(cat.embeddingText());
            String vectorStr = classifierService.toVectorString(embedding);
            categoryRepository.insertWithEmbeddings(cat.label(), cat.embeddingText(), vectorStr);
        }
        log.info("Categories seeded successfully");
    }
}

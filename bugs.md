the docs was written by the BUG tester. There are some bug in the apps. 
1. Invoice generator bug:
    * when user signature in full screen then click on confirm signature the screen rotated and back mini signature pad but there are no signature in the pad. But when user 1st signature in the mini pad then click the full screen the signature was show fine in the screen. But when user click the confirm signature the rotation was happend and signature was gone.\
    * when the user click on the full screen then back to the generate invoice generator screen the Amount Paid Upfront value is gone.
    * there are no system to generate the DRAFT invoice when user did not pay full amount generate the DRAFT invoice.
    * In the PDF invoice there are issues. Always show the Remaining due in big. But in the production grade invoice generator always show the amount paid if the user paid the full amount. if the user paid some money then show the paid amount and DUE amount.
    * The invoice in not updating after the invoice creation. If one client pay their amount after some days the invoice was not updating. its showing only the data when the invoice was created. But in production it was bad. When clients pay the amount update the invoice properly.
    * Now the Invoice screen store the full PDF. When user click on invoice items its show an pdf but now the system was store the full pdf. But in production not store the full pdf. Generate the PDF when user click on the invoice items.

2. add items _> select items screen bug:
   * the screen show the inventory items perfectly but when user click on the add its show - 1 + there things but when user click the + - icons the 1 digit is not updating.
3. Qoutaion screen BUG:
   * Now the apps create qoutaion it looks like the same as invoice its show the serial number and signature in the quotaion creation. But in production the serial number and signature is not need.
4. Clients Screen Bug:
    * when user open the clients items and navigate to the deatils screen then back the client screen and click on the + (add) fab icons the edit clients info open. Its an bug. The + (add) only used for create new clients.
    * in the clients details screen there are missing of information. in production there was need an system for generate invoice, record the payment, open service tickets, uplift the pipeline, generate the qoutaion.
    * in production the clients info are tracked and showed in the details screen. Now its only show the activity of payment and invoice but in production store all the interaction and pipeline time stamp, service ticket time stamp etc and show them in the clients details screen.
5. Pipeline Screen Bug:
    * Now the pipeline screen is not remember its las position. When user won tab and click an items then back pres they tab are going to the lead. But in production must remember the last positon.
6. Inventory improvement:
   * in production show the barcode scaner in the search input fields right section. used only for the items search.
7. Design system:
    * Now the full apps use their own design system. All the thing are not similar to each other. but in production must follow the same design system. 
8. home payment improvement:
    * Now when user click the home screen payment its show the payment history. its not improtant thing. in production must make the payment system on the same screen. in the screen user 1st search the client then select the cients and invoice then add paymetns.

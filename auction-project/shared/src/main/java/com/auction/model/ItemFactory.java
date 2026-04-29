public interface ItemFactory {

    // 🔥 nhận DTO → tạo object
    Item createItem(ItemData data);
}